package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.http.Handler
import io.javalin.security.RouteRole

enum class Route(val isHttpMethod: Boolean = true) {
    HEAD,
    PATCH,
    OPTIONS,
    GET,
    PUT,
    POST,
    DELETE,
    AFTER(isHttpMethod = false),
    BEFORE(isHttpMethod = false),
}

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