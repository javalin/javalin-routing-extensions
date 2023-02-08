package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.dsl.DefaultDsl.DefaultConfiguration
import io.javalin.community.routing.route
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinConfig
import io.javalin.plugin.Plugin

class DslRoutingPlugin<
    CONFIG : RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>,
    ROUTE : DslRoute<CONTEXT, RESPONSE>,
    CONTEXT,
    RESPONSE : Any
>(
    private val routingDsl: RoutingDsl<CONFIG, ROUTE, CONTEXT, RESPONSE>
) : Plugin {

    private val routes = mutableListOf<ROUTE>()

    override fun apply(app: Javalin) {
        routes
            .sortRoutes()
            .map { route -> route to routingDsl.createHandlerFactory().createHandler(route) }
            .forEach { (route, handler) -> app.route(route.method, route.path, handler) }
    }

    fun routing(init: CONFIG.() -> Unit) {
        val routingConfiguration = routingDsl.createConfigurationSupplier().create()
        routingConfiguration.init()
        this.routes.addAll(routingConfiguration.routes)
    }

    fun routing(vararg routes: DslRoutes<ROUTE, CONTEXT, RESPONSE>) {
        this.routes.addAll(routes.flatMap { it.routes() })
    }

    fun routing(vararg routes: ROUTE) {
        this.routes.addAll(routes)
    }

}

fun <
    CONFIG : RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>,
    ROUTE : DslRoute<CONTEXT, RESPONSE>,
    CONTEXT,
    RESPONSE : Any
> JavalinConfig.routing(
    routingDsl: RoutingDsl<CONFIG, ROUTE, CONTEXT, RESPONSE>,
    vararg routes: DslRoutes<ROUTE, CONTEXT, RESPONSE>
) = routing(routingDsl) {
    routes.forEach {
        it.routes().forEach { route ->
            addRoute(route.method, route.path, route.handler)
        }
    }
}

fun JavalinConfig.routing(vararg routes: DefaultRoutes) =
    routing(DefaultDsl, *routes)

fun <
    CONFIG : RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>,
    ROUTE : DslRoute<CONTEXT, RESPONSE>,
    CONTEXT,
    RESPONSE : Any
> JavalinConfig.routing(
    routingDsl: RoutingDsl<CONFIG, ROUTE, CONTEXT, RESPONSE>,
    init: CONFIG.() -> Unit
) {
    val dslPlugin = DslRoutingPlugin(routingDsl)
    dslPlugin.routing(init)
    plugins.register(dslPlugin)
}

fun JavalinConfig.routing(init: DefaultConfiguration.() -> Unit) =
    routing(DefaultDsl, init)