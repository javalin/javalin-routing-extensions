package io.javalin.community.routing.coroutines

import io.javalin.community.routing.RoutingApiInitializer
import io.javalin.community.routing.RoutingSetupScope
import io.javalin.community.routing.coroutines.servlet.CoroutinesServlet
import io.javalin.community.routing.invokeAsSamWithReceiver
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinState
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.router.Endpoint

class Coroutines<ROUTE : SuspendedRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any>(
    private val servlet: CoroutinesServlet<CONTEXT, RESPONSE>,
) : RoutingApiInitializer<CoroutinesRouting<ROUTE, CONTEXT, RESPONSE>> {

    override fun initialize(state: JavalinState, setup: RoutingSetupScope<CoroutinesRouting<ROUTE, CONTEXT, RESPONSE>>) {
        val coroutinesRouting = CoroutinesRouting<ROUTE, CONTEXT, RESPONSE>()
        setup.invokeAsSamWithReceiver(coroutinesRouting)

        coroutinesRouting
            .routes
            .sortRoutes()
            .map { it to Handler { ctx -> servlet.handle(ctx, it) } }
            .forEach { (route, handler) ->
                state.internalRouter.addHttpEndpoint(
                    Endpoint(
                        method = HandlerType.values().first { it.name == route.method.toString() },
                        path = route.path,
                        handler = handler
                    )
                )
            }
    }

}