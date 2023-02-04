package io.javalin.community.routing.coroutines

import io.javalin.Javalin
import io.javalin.community.routing.coroutines.servlet.CoroutinesServlet
import io.javalin.community.routing.route
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinConfig
import io.javalin.http.Handler
import io.javalin.plugin.Plugin

class ReactiveRoutingPlugin<ROUTE : ReactiveRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any>(
    private val servlet: CoroutinesServlet<CONTEXT, RESPONSE>
) : Plugin {

    private val routes = mutableListOf<ROUTE>()

    override fun apply(app: Javalin) {
        routes
            .sortRoutes()
            .map { it to Handler { ctx -> servlet.handle(ctx, it) } }
            .forEach { (reactiveRoute, handler) -> app.route(reactiveRoute.method, reactiveRoute.path, handler) }
    }

    fun <ROUTES : ReactiveRoutes<ROUTE, CONTEXT, RESPONSE>> routing(vararg reactiveRoutes: ROUTES) {
        reactiveRoutes.forEach {
            routes.addAll(it.routes())
        }
    }

}

fun <ROUTE : ReactiveRoute<CONTEXT, RESPONSE>, ROUTES : ReactiveRoutes<ROUTE, CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> JavalinConfig.reactiveRouting(
    servlet: CoroutinesServlet<CONTEXT, RESPONSE>,
    vararg routes: ROUTES
) {
    val reactiveRoutingPlugin = ReactiveRoutingPlugin<ROUTE, CONTEXT, RESPONSE>(servlet)
    reactiveRoutingPlugin.routing(*routes)
    this.plugins.register(reactiveRoutingPlugin)
}