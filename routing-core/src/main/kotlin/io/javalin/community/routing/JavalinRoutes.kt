package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.http.Handler
import io.javalin.security.RouteRole

enum class RouteMethod(val isHttpMethod: Boolean = true) {
    HEAD,
    PATCH,
    OPTIONS,
    GET,
    PUT,
    POST,
    DELETE,
    AFTER(isHttpMethod = false),
    BEFORE(isHttpMethod = false)
}

fun Javalin.route(method: RouteMethod, path: String, handler: Handler, vararg roles: RouteRole) {
    when (method) {
        RouteMethod.HEAD -> head(path, handler, *roles)
        RouteMethod.PATCH -> patch(path, handler, *roles)
        RouteMethod.OPTIONS -> options(path, handler, *roles)
        RouteMethod.GET -> get(path, handler, *roles)
        RouteMethod.PUT -> put(path, handler, *roles)
        RouteMethod.POST -> post(path, handler, *roles)
        RouteMethod.DELETE -> delete(path, handler, *roles)
        RouteMethod.AFTER -> after(path, handler)
        RouteMethod.BEFORE -> before(path, handler)
    }
}