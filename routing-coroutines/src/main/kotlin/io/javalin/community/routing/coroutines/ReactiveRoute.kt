package io.javalin.community.routing.coroutines

import io.javalin.community.routing.Route
import io.javalin.community.routing.Routed
import io.javalin.community.routing.Routes

class ReactiveRoute<CONTEXT, RESPONSE : Any>(
    override val path: String,
    val method: Route,
    val async: Boolean = true,
    val handler: suspend CONTEXT.() -> RESPONSE
) : Routed

abstract class ReactiveRoutes<ROUTE : ReactiveRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> : Routes<ROUTE, CONTEXT, RESPONSE> {

    fun reactiveRoute(path: String, method: Route, async: Boolean = true, handler: suspend CONTEXT.() -> RESPONSE): ReactiveRoute<CONTEXT, RESPONSE> =
        ReactiveRoute(
            path = path,
            method = method,
            async = async,
            handler = handler
        )

}
