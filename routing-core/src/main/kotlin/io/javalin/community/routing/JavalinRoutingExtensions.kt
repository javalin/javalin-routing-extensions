package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.http.Handler
import io.javalin.security.RouteRole

class JavalinRoutingExtensions(private val javalin: Javalin) {

    private val routes = mutableListOf<HandlerEntry>()

    fun addRouteHandler(vararg handlerEntry: HandlerEntry): JavalinRoutingExtensions = also {
        routes.addAll(handlerEntry)
    }

    @JvmOverloads
    fun addRoute(route: Route, path: String, vararg roles: RouteRole = emptyArray(), handler: Handler): JavalinRoutingExtensions = also {
        routes.add(HandlerEntry(route, path, handler, roles.toList()))
    }

    fun register(): Javalin {
        routes
            .sortRoutes()
            .forEach { javalin.registerRoute(it) }

        return javalin
    }

}

data class HandlerEntry @JvmOverloads constructor(
    val route: Route,
    override val path: String,
    val handler: Handler,
    val roles: List<RouteRole> = emptyList(),
) : Routed

fun Javalin.registerRoute(handlerEntry: HandlerEntry) =
    registerRoute(handlerEntry.route, handlerEntry.path, handlerEntry.handler, *handlerEntry.roles.toTypedArray())

fun Javalin.registerRoute(route: Route, path: String, handler: Handler, vararg roles: RouteRole) {
    when (route) {
        Route.HEAD -> head(path, handler, *roles)
        Route.PATCH -> patch(path, handler, *roles)
        Route.OPTIONS -> options(path, handler, *roles)
        Route.GET -> get(path, handler, *roles)
        Route.PUT -> put(path, handler, *roles)
        Route.POST -> post(path, handler, *roles)
        Route.DELETE -> delete(path, handler, *roles)
        Route.AFTER -> after(path, handler)
        Route.BEFORE -> before(path, handler)
    }
}