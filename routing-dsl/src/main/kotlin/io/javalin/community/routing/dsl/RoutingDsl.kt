package io.javalin.community.routing.dsl

import io.javalin.community.routing.RouteMethod
import io.javalin.community.routing.Routed
import io.javalin.community.routing.Routes
import io.javalin.http.Handler

data class DslRoute<CONTEXT, RESPONSE : Any>(
    val method: RouteMethod,
    override val path: String,
    val handler: CONTEXT.() -> RESPONSE
) : Routed

fun interface DslRoutes<ROUTE : DslRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> : Routes<ROUTE, CONTEXT, RESPONSE> {

    fun route(path: String, method: RouteMethod, handler: CONTEXT.() -> RESPONSE): DslRoute<CONTEXT, RESPONSE> =
        DslRoute(
            path = path,
            method = method,
            handler = handler
        )

}

interface RoutingDsl<CONFIG : RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>, ROUTE : DslRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> {

    fun createConfigurationSupplier(): ConfigurationSupplier<CONFIG, ROUTE, CONTEXT, RESPONSE>

    fun createHandlerFactory(): HandlerFactory<ROUTE>

}

fun interface HandlerFactory<ROUTE : Routed> {
    fun createHandler(route: ROUTE): Handler
}

fun interface ConfigurationSupplier<CONFIG : RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>, ROUTE : DslRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> {
    fun create(): CONFIG
}

open class RoutingConfiguration<ROUTE : DslRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> {

    val routes = mutableSetOf<ROUTE>()

    fun get(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.GET, path, handler)
    fun post(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.POST, path, handler)
    fun put(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.PUT, path, handler)
    fun delete(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.DELETE, path, handler)
    fun patch(path: String, handler: CONTEXT.() -> RESPONSE)  = addRoute(RouteMethod.PATCH, path, handler)
    fun head(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.HEAD, path, handler)
    fun options(path: String, handler: CONTEXT.() -> RESPONSE) =  addRoute(RouteMethod.OPTIONS, path, handler)
    fun before(path: String = "", handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.BEFORE, path, handler)
    fun after(path: String = "", handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.AFTER, path, handler)

    @Suppress("UNCHECKED_CAST")
    fun addRoute(method: RouteMethod, path: String, handler: CONTEXT.() -> RESPONSE) {
        val route = DslRoute(method, path, handler) as ROUTE
        routes.add(route)
    }

}