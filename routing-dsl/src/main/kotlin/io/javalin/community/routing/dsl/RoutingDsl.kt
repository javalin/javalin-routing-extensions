package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.HandlerFactory
import io.javalin.community.routing.RouteEntry
import io.javalin.community.routing.RouteMethod
import io.javalin.community.routing.registerRoutes
import io.javalin.config.JavalinConfig
import io.javalin.plugin.Plugin

class DslRoutingPlugin<CONFIG : RoutingConfiguration<CONTEXT, RESPONSE>, CONTEXT, RESPONSE>(
    private val configurationSupplier: ConfigurationSupplier<CONFIG, CONTEXT, RESPONSE>,
    private val contextFactory: HandlerFactory<CONTEXT, RESPONSE>
) : Plugin {

    private val routes = mutableListOf<RouteEntry<CONTEXT, RESPONSE>>()

    override fun apply(app: Javalin) {
        app.registerRoutes(routes, contextFactory)
    }

    fun routing(init: CONFIG.() -> Unit) {
        val routingConfiguration = configurationSupplier.create()
        routingConfiguration.init()
        routes.addAll(routingConfiguration.routes)
    }

}

fun interface ConfigurationSupplier<CONFIG : RoutingConfiguration<CONTEXT, RESPONSE>, CONTEXT, RESPONSE> {
    fun create(): CONFIG
}

fun <CONFIG : RoutingConfiguration<CONTEXT, RESPONSE>, CONTEXT, RESPONSE> JavalinConfig.routing(
    routingDsl: RoutingDsl<CONFIG, CONTEXT, RESPONSE>,
    init: CONFIG.() -> Unit
) = routing(routingDsl.createConfigurationSupplier(), routingDsl.createHandlerFactory(), init)

fun <CONFIG : RoutingConfiguration<CONTEXT, RESPONSE>, CONTEXT, RESPONSE> JavalinConfig.routing(
    configurationSupplier: ConfigurationSupplier<CONFIG, CONTEXT, RESPONSE>,
    contextFactory: HandlerFactory<CONTEXT, RESPONSE>,
    init: CONFIG.() -> Unit
) {
    val dslPlugin = DslRoutingPlugin(configurationSupplier, contextFactory)
    dslPlugin.routing(init)
    plugins.register(dslPlugin)
}

open class RoutingConfiguration<CONTEXT, RESPONSE> {

    internal val routes = mutableSetOf<RouteEntry<CONTEXT, RESPONSE>>()

    fun get(path: String, handler: CONTEXT.() -> RESPONSE) {
        routes.add(RouteEntry(RouteMethod.GET, path, handler))
    }

    fun post(path: String, handler: CONTEXT.() -> RESPONSE) {
        routes.add(RouteEntry(RouteMethod.POST, path, handler))
    }

    fun put(path: String, handler: CONTEXT.() -> RESPONSE) {
        routes.add(RouteEntry(RouteMethod.PUT, path, handler))
    }

    fun delete(path: String, handler: CONTEXT.() -> RESPONSE) {
        routes.add(RouteEntry(RouteMethod.DELETE, path, handler))
    }

    fun patch(path: String, handler: CONTEXT.() -> RESPONSE) {
        routes.add(RouteEntry(RouteMethod.PATCH, path, handler))
    }

    fun head(path: String, handler: CONTEXT.() -> RESPONSE) {
        routes.add(RouteEntry(RouteMethod.HEAD, path, handler))
    }

    fun options(path: String, handler: CONTEXT.() -> RESPONSE) {
        routes.add(RouteEntry(RouteMethod.OPTIONS, path, handler))
    }

    fun before(path: String = "", handler: CONTEXT.() -> RESPONSE) {
        routes.add(RouteEntry(RouteMethod.BEFORE, path, handler))
    }

    fun after(path: String = "", handler: CONTEXT.() -> RESPONSE) {
        routes.add(RouteEntry(RouteMethod.AFTER, path, handler))
    }

}

interface RoutingDsl<CONFIG : RoutingConfiguration<CONTEXT, RESPONSE>, CONTEXT, RESPONSE> {

    fun createConfigurationSupplier(): ConfigurationSupplier<CONFIG, CONTEXT, RESPONSE>

    fun createHandlerFactory(): HandlerFactory<CONTEXT, RESPONSE>

}
