package io.javalin.community.routing.coroutines

import io.javalin.community.routing.coroutines.servlet.CoroutinesServlet
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinConfig
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.router.InternalRouter
import io.javalin.router.RoutingApiInitializer
import java.util.function.Consumer

class Coroutines<ROUTE : SuspendedRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any>(
    private val servlet: CoroutinesServlet<CONTEXT, RESPONSE>,
) : RoutingApiInitializer<CoroutinesRouting<ROUTE, CONTEXT, RESPONSE>> {

    override fun initialize(cfg: JavalinConfig, internalRouter: InternalRouter, setup: Consumer<CoroutinesRouting<ROUTE, CONTEXT, RESPONSE>>) {
        val coroutinesRouting = CoroutinesRouting<ROUTE, CONTEXT, RESPONSE>()
        setup.accept(coroutinesRouting)

        coroutinesRouting
            .routes
            .sortRoutes()
            .map { it to Handler { ctx -> servlet.handle(ctx, it) } }
            .forEach { (route, handler) -> internalRouter.addHttpHandler(HandlerType.valueOf(route.method.toString()), route.path, handler) }
    }

}