package io.javalin.community.routing.coroutines

import io.javalin.community.routing.Route

class CoroutinesRouting<ROUTE : SuspendedRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> {

    internal val routes = mutableListOf<ROUTE>()

    fun route(route: ROUTE) {
        routes.add(route)
    }

    fun register(exampleEndpoint: SuspendedRoutes<ROUTE, CONTEXT, RESPONSE>): CoroutinesRouting<ROUTE, CONTEXT, RESPONSE> {
        exampleEndpoint.routes().forEach { route(it) }
        return this
    }

}

fun <CONTEXT, RESPONSE : Any> CoroutinesRouting<SuspendedRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE>.route(
    method: Route,
    path: String,
    async: Boolean = true,
    handler: suspend CONTEXT.() -> RESPONSE
) {
    routes.add(SuspendedRoute(path, method, async, handler))
}
