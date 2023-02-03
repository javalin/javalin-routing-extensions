package io.javalin.community.routing.dsl.examples

import io.javalin.Javalin
import io.javalin.community.routing.dsl.ConfigurationSupplier
import io.javalin.community.routing.dsl.DefaultContextScope
import io.javalin.community.routing.dsl.DefaultContextScopeConfiguration
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.dsl.HandlerFactory
import io.javalin.community.routing.dsl.Path
import io.javalin.community.routing.dsl.RoutingDsl
import io.javalin.community.routing.dsl.examples.CustomDsl.CustomScope
import io.javalin.community.routing.dsl.examples.CustomDsl.CustomRoutingConfiguration
import io.javalin.community.routing.dsl.routing
import io.javalin.http.Context
import io.javalin.http.Handler

object CustomDsl : RoutingDsl<CustomRoutingConfiguration, DslRoute<CustomScope, Unit>, CustomScope, Unit> {

    // This is custom configuration class that will be used to register routes
    open class CustomRoutingConfiguration : DefaultContextScopeConfiguration<DslRoute<CustomScope, Unit>, CustomScope, Unit>()

    // This is custom scope that will be used in the handlers
    class CustomScope(
        override val ctx: Context,
    ): DefaultContextScope, Context by ctx {
        fun helloWorld(): String = "Hello ${ctx.endpointHandlerPath()}"
    }

    override fun createConfigurationSupplier(): ConfigurationSupplier<CustomRoutingConfiguration, DslRoute<CustomScope, Unit>, CustomScope, Unit> =
        ConfigurationSupplier{ CustomRoutingConfiguration() }

    override fun createHandlerFactory(): HandlerFactory<DslRoute<CustomScope, Unit>> =
        HandlerFactory { route ->
            Handler {
                route.handler.invoke(CustomScope(it))
            }
        }

}

@Path("/panda/{age}")
data class PandaPath(val age: Int)

fun main() {
    Javalin.create { config ->
        config.routing(CustomDsl) {
            before {
                // `endpointHandlerPath` comes from Context class
                result("Called endpoint: ${matchedPath()}")
            }
            get("/") {
                // `helloWorld` comes from CustomScope class
                result(helloWorld())
            }
            get<PandaPath> { path ->
                // support for type-safe paths
                result(path.age.toString())
            }
        }
    }.start(8080)
}