package io.javalin.community.routing.dsl

import io.javalin.community.routing.Route
import io.javalin.community.routing.Routed
import io.javalin.community.routing.Routes
import io.javalin.http.Handler

interface DslRoute<CONTEXT, RESPONSE : Any> : Routed {
    val method: Route
    val version: String?
    val handler: CONTEXT.() -> RESPONSE
}

open class DefaultDslRoute<CONTEXT, RESPONSE : Any>(
    override val method: Route,
    override val path: String,
    override val version: String? = null,
    override val handler: CONTEXT.() -> RESPONSE
) : DslRoute<CONTEXT, RESPONSE>

fun interface DslRoutes<ROUTE : DslRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> : Routes<ROUTE, CONTEXT, RESPONSE> {

    fun route(path: String, method: Route, handler: CONTEXT.() -> RESPONSE): DslRoute<CONTEXT, RESPONSE> =
        DefaultDslRoute(
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

    fun get(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(Route.GET, path, handler)
    fun post(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(Route.POST, path, handler)
    fun put(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(Route.PUT, path, handler)
    fun delete(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(Route.DELETE, path, handler)
    fun patch(path: String, handler: CONTEXT.() -> RESPONSE)  = addRoute(Route.PATCH, path, handler)
    fun head(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(Route.HEAD, path, handler)
    fun options(path: String, handler: CONTEXT.() -> RESPONSE) =  addRoute(Route.OPTIONS, path, handler)
    fun before(path: String = "", handler: CONTEXT.() -> RESPONSE) = addRoute(Route.BEFORE, path, handler)
    fun after(path: String = "", handler: CONTEXT.() -> RESPONSE) = addRoute(Route.AFTER, path, handler)

    fun addRoute(method: Route, path: String, handler: CONTEXT.() -> RESPONSE) {
        addRoute(
            DefaultDslRoute(
                method = method,
                path = path,
                handler = handler
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun addRoute(route: DslRoute<CONTEXT, RESPONSE>) {
        routes.add(route as ROUTE)
    }

}