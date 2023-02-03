package io.javalin.community.routing.dsl

import io.javalin.community.routing.dsl.DefaultDsl.DefaultConfiguration
import io.javalin.community.routing.dsl.DefaultDsl.DefaultScope
import io.javalin.http.Context
import io.javalin.http.Handler

typealias DefaultRoute = DslRoute<DefaultScope, Unit>

abstract class DefaultRoutes : DslRoutes<DefaultRoute, DefaultScope, Unit>

/**
 * Default implementation of [RoutingDsl] that uses [DefaultScope] as scope and [DefaultConfiguration] as configuration.
 */
object DefaultDsl : RoutingDsl<DefaultConfiguration, DslRoute<DefaultScope, Unit>, DefaultScope, Unit> {

    open class DefaultConfiguration : DefaultContextScopeConfiguration<DslRoute<DefaultScope, Unit>, DefaultScope, Unit>()

    open class DefaultScope(override val ctx: Context) : DefaultContextScope, Context by ctx

    override fun createConfigurationSupplier(): ConfigurationSupplier<DefaultConfiguration, DefaultRoute, DefaultScope, Unit> =
        ConfigurationSupplier { DefaultConfiguration() }

    override fun createHandlerFactory(): HandlerFactory<DefaultRoute> =
        HandlerFactory { route ->
            Handler { ctx ->
                route.handler(DefaultScope(ctx))
            }
        }

}
