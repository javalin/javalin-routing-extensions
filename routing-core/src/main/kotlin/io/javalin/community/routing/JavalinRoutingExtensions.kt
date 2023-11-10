package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.router.InternalRouter
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
            .forEach { javalin.unsafeConfig().pvt.internalRouter.registerRoute(it) }

        return javalin
    }

}

data class HandlerEntry @JvmOverloads constructor(
    val route: Route,
    override val path: String,
    val handler: Handler,
    val roles: List<RouteRole> = emptyList(),
) : Routed

fun InternalRouter.registerRoute(handlerEntry: HandlerEntry) =
    registerRoute(handlerEntry.route, handlerEntry.path, handlerEntry.handler, *handlerEntry.roles.toTypedArray())

fun InternalRouter.registerRoute(route: Route, path: String, handler: Handler, vararg roles: RouteRole) {
    when (route) {
        Route.HEAD -> addHttpHandler(HandlerType.HEAD, path, handler, *roles)
        Route.PATCH -> addHttpHandler(HandlerType.PATCH, path, handler, *roles)
        Route.OPTIONS -> addHttpHandler(HandlerType.OPTIONS, path, handler, *roles)
        Route.GET -> addHttpHandler(HandlerType.GET, path, handler, *roles)
        Route.PUT -> addHttpHandler(HandlerType.PUT, path, handler, *roles)
        Route.POST -> addHttpHandler(HandlerType.POST, path, handler, *roles)
        Route.DELETE -> addHttpHandler(HandlerType.DELETE, path, handler, *roles)
        Route.AFTER -> addHttpHandler(HandlerType.AFTER, path, handler)
        Route.BEFORE -> addHttpHandler(HandlerType.BEFORE, path, handler)
    }
}