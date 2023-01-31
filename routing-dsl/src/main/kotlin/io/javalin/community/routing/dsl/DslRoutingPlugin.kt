package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.RouteMethod.AFTER
import io.javalin.community.routing.RouteMethod.BEFORE
import io.javalin.community.routing.RouteMethod.DELETE
import io.javalin.community.routing.RouteMethod.GET
import io.javalin.community.routing.RouteMethod.HEAD
import io.javalin.community.routing.RouteMethod.OPTIONS
import io.javalin.community.routing.RouteMethod.PATCH
import io.javalin.community.routing.RouteMethod.POST
import io.javalin.community.routing.RouteMethod.PUT
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
            .forEach { (route, handler) ->
                when (route.method) {
                    HEAD -> app.head(route.path, handler)
                    PATCH -> app.patch(route.path, handler)
                    OPTIONS -> app.options(route.path, handler)
                    GET -> app.get(route.path, handler)
                    PUT -> app.put(route.path, handler)
                    POST -> app.post(route.path, handler)
                    DELETE -> app.delete(route.path, handler)
                    AFTER -> app.after(route.path, handler)
                    BEFORE -> app.before(route.path, handler)
                }
            }
    }

    fun routing(init: CONFIG.() -> Unit) {
        val routingConfiguration = routingDsl.createConfigurationSupplier().create()
        routingConfiguration.init()
        routes.addAll(routingConfiguration.routes)
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