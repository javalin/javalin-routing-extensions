package io.javalin.community.routing.dsl

import io.javalin.community.routing.HandlerFactory
import io.javalin.community.routing.Route
import io.javalin.community.routing.RouteMethod

interface RoutingDsl<CONFIG : RoutingConfiguration<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> {

    fun createConfigurationSupplier(): ConfigurationSupplier<CONFIG, CONTEXT, RESPONSE>

    fun createHandlerFactory(): HandlerFactory<CONTEXT, RESPONSE>

}

fun interface ConfigurationSupplier<CONFIG : RoutingConfiguration<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> {
    fun create(): CONFIG
}

open class RoutingConfiguration<CONTEXT, RESPONSE : Any> {

    internal val routes = mutableSetOf<Route<CONTEXT, RESPONSE>>()

    fun get(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.GET, path, handler)
    fun post(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.POST, path, handler)
    fun put(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.PUT, path, handler)
    fun delete(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.DELETE, path, handler)
    fun patch(path: String, handler: CONTEXT.() -> RESPONSE)  = addRoute(RouteMethod.PATCH, path, handler)
    fun head(path: String, handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.HEAD, path, handler)
    fun options(path: String, handler: CONTEXT.() -> RESPONSE) =  addRoute(RouteMethod.OPTIONS, path, handler)
    fun before(path: String = "", handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.BEFORE, path, handler)
    fun after(path: String = "", handler: CONTEXT.() -> RESPONSE) = addRoute(RouteMethod.AFTER, path, handler)

    fun addRoute(method: RouteMethod, path: String, handler: CONTEXT.() -> RESPONSE) {
        routes.add(Route(method, path, handler))
    }

}