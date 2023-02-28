package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.dsl.defaults.DefaultDsl
import io.javalin.community.routing.dsl.defaults.DefaultDsl.DefaultConfiguration
import io.javalin.community.routing.dsl.defaults.DefaultRoutes
import io.javalin.community.routing.registerRoute
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinConfig
import io.javalin.plugin.Plugin
import kotlin.reflect.KClass

class DslRoutingPlugin<
    CONFIG : RoutingDslConfiguration<ROUTE, CONTEXT, RESPONSE>,
    ROUTE : DslRoute<CONTEXT, RESPONSE>,
    CONTEXT,
    RESPONSE : Any
>(
    private val routingDsl: RoutingDslFactory<CONFIG, ROUTE, CONTEXT, RESPONSE>
) : Plugin {

    private val registeredRoutes = mutableListOf<ROUTE>()
    private val registeredExceptionHandlers = mutableMapOf<KClass<out Exception>, DslExceptionHandler<CONTEXT, Exception, RESPONSE>>()

    override fun apply(app: Javalin) {
        registeredRoutes
            .sortRoutes()
            .map { route -> route to routingDsl.createHandler(route) }
            .forEach { (route, handler) -> app.registerRoute(route.method, route.path, handler) }

        registeredExceptionHandlers.forEach { (exceptionClass, handler) ->
            app.exception(exceptionClass.java, routingDsl.createExceptionHandler(handler))
        }
    }

    fun routing(init: CONFIG.() -> Unit): DslRoutingPlugin<CONFIG, ROUTE, CONTEXT, RESPONSE> = also {
        val routingConfiguration = routingDsl.createConfiguration()
        routingConfiguration.init()
        registeredRoutes.addAll(routingConfiguration.routes)
        registeredExceptionHandlers.putAll(routingConfiguration.exceptionHandlers)
    }

    fun routing(vararg containers: DslContainer<ROUTE, CONTEXT, RESPONSE>): DslRoutingPlugin<CONFIG, ROUTE, CONTEXT, RESPONSE> =
        routing {
            containers.forEach { container ->
                addRoutes(container.routes())
                container.exceptionHandlers().forEach { exception(it.type, it.handler) }
            }
        }

    fun routing(routes: Collection<ROUTE>): DslRoutingPlugin<CONFIG, ROUTE, CONTEXT, RESPONSE> =
        routing {
            addRoutes(routes)
        }

    fun routing(vararg routes: ROUTE): DslRoutingPlugin<CONFIG, ROUTE, CONTEXT, RESPONSE> =
        routing(routes.toList())

}

fun <
    CONFIG : RoutingDslConfiguration<ROUTE, CONTEXT, RESPONSE>,
    ROUTE : DslRoute<CONTEXT, RESPONSE>,
    CONTEXT,
    RESPONSE : Any
> JavalinConfig.routing(
    routingDsl: RoutingDslFactory<CONFIG, ROUTE, CONTEXT, RESPONSE>,
    vararg routes: DslContainer<ROUTE, CONTEXT, RESPONSE>
) = plugins.register(
    DslRoutingPlugin(routingDsl).routing(*routes)
)

fun JavalinConfig.routing(vararg routes: DefaultRoutes) =
    routing(DefaultDsl, *routes)

fun <
    CONFIG : RoutingDslConfiguration<ROUTE, CONTEXT, RESPONSE>,
    ROUTE : DslRoute<CONTEXT, RESPONSE>,
    CONTEXT,
    RESPONSE : Any
> JavalinConfig.routing(
    routingDsl: RoutingDslFactory<CONFIG, ROUTE, CONTEXT, RESPONSE>,
    init: CONFIG.() -> Unit
) = plugins.register(
    DslRoutingPlugin(routingDsl).routing(init)
)

fun JavalinConfig.routing(init: DefaultConfiguration.() -> Unit) =
    routing(DefaultDsl, init)