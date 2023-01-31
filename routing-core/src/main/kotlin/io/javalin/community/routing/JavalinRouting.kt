package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.community.routing.RouteMethod.AFTER
import io.javalin.community.routing.RouteMethod.BEFORE
import io.javalin.community.routing.RouteMethod.DELETE
import io.javalin.community.routing.RouteMethod.GET
import io.javalin.community.routing.RouteMethod.HEAD
import io.javalin.community.routing.RouteMethod.OPTIONS
import io.javalin.community.routing.RouteMethod.PATCH
import io.javalin.community.routing.RouteMethod.POST
import io.javalin.community.routing.RouteMethod.PUT
import io.javalin.http.Handler

fun interface HandlerFactory<CONTEXT, RESPONSE> {
    fun createHandler(route: Route<CONTEXT, RESPONSE>): Handler
}

fun <CONTEXT, RESPONSE> Javalin.registerRoutes(
    routes: List<Route<CONTEXT, RESPONSE>>,
    handlerFactory: HandlerFactory<CONTEXT, RESPONSE>
) {
    routes
        .sortedWith(RouteComparator())
        .map { route -> Pair(route, handlerFactory.createHandler(route)) }
        .forEach { (route, handler) ->
            when (route.method) {
                HEAD -> head(route.path, handler)
                PATCH -> patch(route.path, handler)
                OPTIONS -> options(route.path, handler)
                GET -> get(route.path, handler)
                PUT -> put(route.path, handler)
                POST -> post(route.path, handler)
                DELETE -> delete(route.path, handler)
                AFTER -> after(route.path, handler)
                BEFORE -> before(route.path, handler)
            }
        }
}