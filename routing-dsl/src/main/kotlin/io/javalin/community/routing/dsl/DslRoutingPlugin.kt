package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.Route
import io.javalin.community.routing.Routes
import io.javalin.community.routing.registerRoutes
import io.javalin.config.JavalinConfig
import io.javalin.plugin.Plugin

class DslRoutingPlugin<CONFIG : RoutingConfiguration<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any>(
    private val routingDsl: RoutingDsl<CONFIG, CONTEXT, RESPONSE>
) : Plugin {

    private val routes = mutableListOf<Route<CONTEXT, RESPONSE>>()

    override fun apply(app: Javalin) {
        app.registerRoutes(routes, routingDsl.createHandlerFactory())
    }

    fun routing(init: CONFIG.() -> Unit) {
        val routingConfiguration = routingDsl.createConfigurationSupplier().create()
        routingConfiguration.init()
        routes.addAll(routingConfiguration.routes)
    }

}

fun <CONFIG : RoutingConfiguration<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> JavalinConfig.routing(
    routingDsl: RoutingDsl<CONFIG, CONTEXT, RESPONSE>,
    vararg routes: Routes<CONTEXT, RESPONSE>
) = routing(routingDsl) {
    routes.forEach {
        it.routes().forEach { route ->
            addRoute(route.method, route.path, route.handler)
        }
    }
}

fun <CONFIG : RoutingConfiguration<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> JavalinConfig.routing(
    routingDsl: RoutingDsl<CONFIG, CONTEXT, RESPONSE>,
    init: CONFIG.() -> Unit
) {
    val dslPlugin = DslRoutingPlugin(routingDsl)
    dslPlugin.routing(init)
    plugins.register(dslPlugin)
}