package io.javalin.community.routing.dsl.examples

import io.javalin.Javalin
import io.javalin.community.routing.dsl.DslExceptionHandler
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.dsl.DslRouting
import io.javalin.community.routing.dsl.RoutingDslFactory
import io.javalin.community.routing.dsl.defaults.DefaultContextScopeConfiguration
import io.javalin.community.routing.dsl.defaults.DefaultDsl.DefaultScope
import io.javalin.community.routing.dsl.defaults.Path
import io.javalin.community.routing.dsl.examples.ExampleDsl.CustomRoutingConfiguration
import io.javalin.community.routing.dsl.examples.ExampleDsl.CustomScope
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler

object ExampleDsl : RoutingDslFactory<CustomRoutingConfiguration, DslRoute<CustomScope, Unit>, CustomScope, Unit> {

    // This is custom configuration class that will be used to register routes
    open class CustomRoutingConfiguration : DefaultContextScopeConfiguration<DslRoute<CustomScope, Unit>, CustomScope, Unit>()

    // This is custom scope that will be used in the handlers
    class CustomScope(ctx: Context) : DefaultScope(ctx) {
        fun helloWorld(): String = "Hello ${ctx.endpointHandlerPath()}"
    }

    override fun createConfiguration(): CustomRoutingConfiguration =
        CustomRoutingConfiguration()

    override fun createHandler(route: DslRoute<CustomScope, Unit>): Handler =
        Handler {
            route.handler.invoke(CustomScope(it))
        }

    override fun createExceptionHandler(handler: DslExceptionHandler<CustomScope, Exception, Unit>): ExceptionHandler<Exception> =
        ExceptionHandler { exception, ctx ->
            handler.invoke(CustomScope(ctx), exception)
        }

}

@Path("/panda/{age}")
data class PandaPath(val age: Int)

fun main() {
    Javalin.create { config ->
        config.router.mount(DslRouting(ExampleDsl)) {
            it.before {
                // `endpointHandlerPath` comes from Context class
                result("Called endpoint: ${matchedPath()}")
            }
            it.get("/") {
                // `helloWorld` comes from CustomScope class
                result(helloWorld())
            }
            it.get<PandaPath> { path ->
                // support for type-safe paths
                result(path.age.toString())
            }
            it.exception(Exception::class) { anyException ->
                // support for exception handlers
                result(anyException.message ?: "Unknown error")
            }
        }
    }.start(8080)
}