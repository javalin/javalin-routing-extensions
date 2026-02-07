package io.javalin.community.routing.dsl.defaults

import io.javalin.community.routing.dsl.DslExceptionHandler
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.dsl.DslContainer
import io.javalin.community.routing.dsl.RoutingDslFactory
import io.javalin.community.routing.dsl.defaults.DefaultDsl.DefaultConfiguration
import io.javalin.community.routing.dsl.defaults.DefaultDsl.DefaultScope
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler

typealias DefaultRoute = DslRoute<DefaultScope, Unit>

abstract class DefaultRouting : DslContainer<DefaultRoute, DefaultScope, Unit>

/**
 * Default implementation of [RoutingDslFactory] that uses [DefaultScope] as scope and [DefaultConfiguration] as configuration.
 */
object DefaultDsl : RoutingDslFactory<DefaultConfiguration, DslRoute<DefaultScope, Unit>, DefaultScope, Unit> {

    open class DefaultConfiguration : DefaultContextScopeConfiguration<DslRoute<DefaultScope, Unit>, DefaultScope, Unit>()

    open class DefaultScope(override val ctx: Context) : DefaultContextScope, Context by ctx

    override fun createConfiguration(): DefaultConfiguration =
        DefaultConfiguration()

    override fun createHandler(route: DslRoute<DefaultScope, Unit>): Handler =
        Handler { ctx ->
            route.handler(DefaultScope(ctx))
        }

    override fun createExceptionHandler(handler: DslExceptionHandler<DefaultScope, Exception, Unit>): ExceptionHandler<Exception> =
        ExceptionHandler { exception, ctx ->
            handler(DefaultScope(ctx), exception)
        }

}
