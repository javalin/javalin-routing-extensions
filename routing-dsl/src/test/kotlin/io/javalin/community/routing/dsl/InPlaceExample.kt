package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.HandlerFactory
import io.javalin.community.routing.dsl.CustomDsl.CustomScope
import io.javalin.community.routing.dsl.CustomDsl.CustomRoutingConfiguration
import io.javalin.http.Context
import io.javalin.http.Handler

object CustomDsl : RoutingDsl<CustomRoutingConfiguration, CustomScope, Unit> {

    // This is custom configuration class that will be used to register routes
    open class CustomRoutingConfiguration : DefaultContextScopeConfiguration<CustomScope, Unit>()

    // This is custom scope that will be used in the handlers
    class CustomScope(
        override val ctx: Context,
    ): DefaultContextScope, Context by ctx {
        fun helloWorld(): String = "Hello ${ctx.endpointHandlerPath()}"
    }

    override fun createConfigurationSupplier(): ConfigurationSupplier<CustomRoutingConfiguration, CustomScope, Unit> =
        ConfigurationSupplier{ CustomRoutingConfiguration() }

    override fun createHandlerFactory(): HandlerFactory<CustomScope, Unit> =
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
                result("Called endpoint: ${endpointHandlerPath()}")
            }
            get("/") {
                // `helloWorld` comes from CustomScope class
                result("Hello ${helloWorld()}")
            }
            get<PandaPath> { path ->
                // support for type-safe paths
                result(path.age.toString())
            }
        }
    }.start(8080)
}