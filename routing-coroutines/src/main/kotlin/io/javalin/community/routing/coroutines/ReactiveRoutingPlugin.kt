package io.javalin.community.routing.coroutines

import io.javalin.community.routing.Route
import io.javalin.community.routing.coroutines.servlet.CoroutinesServlet
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinConfig
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.router.InternalRouter
import io.javalin.router.RoutingApiInitializer
import java.util.function.Consumer

class CoroutinesRouting<CONTEXT, RESPONSE : Any> {
    internal val routes = mutableListOf<ReactiveRoute<CONTEXT, RESPONSE>>()

    fun route(route: ReactiveRoute<CONTEXT, RESPONSE>) {
        routes.add(route)
    }

    fun route(method: Route, path: String, async: Boolean = true, handler: suspend CONTEXT.() -> RESPONSE) {
        routes.add(ReactiveRoute(path, method, async, handler))
    }

    fun routes(exampleEndpoint: ReactiveRoutes<ReactiveRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE>): CoroutinesRouting<CONTEXT, RESPONSE> {
        exampleEndpoint.routes().forEach { route(it) }
        return this
    }

}

class Coroutines<ROUTE : ReactiveRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any>(
    private val servlet: CoroutinesServlet<CONTEXT, RESPONSE>,
) : RoutingApiInitializer<CoroutinesRouting<CONTEXT, RESPONSE>> {

    override fun initialize(cfg: JavalinConfig, internalRouter: InternalRouter, setup: Consumer<CoroutinesRouting<CONTEXT, RESPONSE>>) {
        val coroutinesRouting = CoroutinesRouting<CONTEXT, RESPONSE>()
        setup.accept(coroutinesRouting)

        coroutinesRouting
            .routes
            .sortRoutes()
            .map { it to Handler { ctx -> servlet.handle(ctx, it) } }
            .forEach { (route, handler) -> internalRouter.addHttpHandler(HandlerType.valueOf(route.method.toString()), route.path, handler) }
    }

}