package io.javalin.community.routing.dsl

import io.javalin.community.routing.RouteMethod

interface Route {
    val path: String
    val methods: List<RouteMethod>
}

interface Routes<ROUTE : Route> {
    val routes: Set<ROUTE>
}

open class StandardRoute<CONTEXT, RESPONSE>(
    override val path: String,
    vararg methods: RouteMethod,
    open val handler: CONTEXT.() -> RESPONSE
) : Route {

    override val methods: List<RouteMethod> =
        methods.toList()

}

abstract class StandardRoutes<CONTEXT, RESPONSE : Any> : Routes<StandardRoute<CONTEXT, RESPONSE>> {

    protected fun route(path: String, vararg methods: RouteMethod, handler: CONTEXT.() -> RESPONSE): StandardRoute<CONTEXT, RESPONSE> =
        StandardRoute(
            path = path,
            methods = methods,
            handler = handler
        )

}
