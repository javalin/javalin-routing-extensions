package com.reposilite.web.coroutines

import com.reposilite.web.routing.Route
import com.reposilite.web.routing.RouteMethod
import com.reposilite.web.routing.Routes

class AsyncRoute<CONTEXT, RESPONSE>(
    override val path: String,
    vararg methods: RouteMethod,
    val async: Boolean = true,
    val handler: suspend CONTEXT.() -> RESPONSE
) : Route {

    override val methods: List<RouteMethod> =
        methods.toList()

}

abstract class AsyncRoutes<CONTEXT, RESPONSE> : Routes<AsyncRoute<CONTEXT, RESPONSE>> {

    fun route(path: String, vararg methods: RouteMethod, async: Boolean = true, handler: suspend CONTEXT.() -> RESPONSE): AsyncRoute<CONTEXT, RESPONSE> =
        AsyncRoute(
            path = path,
            methods = methods,
            async = async,
            handler = handler
        )

}
