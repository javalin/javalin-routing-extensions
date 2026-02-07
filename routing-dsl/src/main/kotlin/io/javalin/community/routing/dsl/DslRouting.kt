package io.javalin.community.routing.dsl

import io.javalin.community.routing.RoutingApiInitializer
import io.javalin.community.routing.RoutingSetupScope
import io.javalin.community.routing.dsl.defaults.DefaultDsl
import io.javalin.community.routing.dsl.defaults.DefaultDsl.DefaultConfiguration
import io.javalin.community.routing.dsl.defaults.DefaultDsl.DefaultScope
import io.javalin.community.routing.dsl.defaults.DefaultRoute
import io.javalin.community.routing.invokeAsSamWithReceiver
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinState
import io.javalin.http.HandlerType
import io.javalin.router.Endpoint

open class DslRouting<
    CONFIG : RoutingDslConfiguration<ROUTE, CONTEXT, RESPONSE>,
    ROUTE : DslRoute<CONTEXT, RESPONSE>,
    CONTEXT,
    RESPONSE : Any
>(
    private val factory: RoutingDslFactory<CONFIG, ROUTE, CONTEXT, RESPONSE>
) : RoutingApiInitializer<CONFIG> {

    companion object {
        object Dsl : DslRouting<DefaultConfiguration, DefaultRoute, DefaultScope, Unit>(DefaultDsl)
    }

    override fun initialize(state: JavalinState, setup: RoutingSetupScope<CONFIG>) {
        val dslConfig = factory.createConfiguration()
        setup.invokeAsSamWithReceiver(dslConfig)

        dslConfig.routes
            .sortRoutes()
            .map { route -> route to factory.createHandler(route) }
            .forEach { (route, handler) ->
                state.internalRouter.addHttpEndpoint(
                    Endpoint(
                        method = HandlerType.values().first { it.name == route.method.toString() },
                        path = route.path,
                        handler = handler
                    )
                )
            }

        dslConfig.exceptionHandlers.forEach { (exceptionClass, handler) ->
            state.internalRouter.addHttpExceptionHandler(exceptionClass.java, factory.createExceptionHandler(handler))
        }
    }

}