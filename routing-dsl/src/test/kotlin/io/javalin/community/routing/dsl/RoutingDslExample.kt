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
        private val customContext: CustomContext
    ): DefaultContextScope, Context by ctx, CustomContext by customContext

    // This is custom context that will be used in the handlers
    interface CustomContext {
        fun helloWorld(): String
    }

    // Implementation of the custom context
    private class CustomContextImpl(private val ctx: Context) : CustomContext {
        override fun helloWorld(): String = "Hello ${ctx.endpointHandlerPath()}"
    }

    override fun createConfigurationSupplier(): ConfigurationSupplier<CustomRoutingConfiguration, CustomScope, Unit> =
        ConfigurationSupplier{ CustomRoutingConfiguration() }

    override fun createHandlerFactory(): HandlerFactory<CustomScope, Unit> =
        HandlerFactory { route ->
            Handler {
                route.handler.invoke(CustomScope(it, CustomContextImpl(it)))
            }
        }

}

@Path("/panda/{age}")
data class PandaPath(val age: Int)

fun main() {
    Javalin.create { config ->
        config.routing(CustomDsl) {
            before {
                println("Called endpoint: ${endpointHandlerPath()}")
            }
            get("/") {
                result("Hello ${helloWorld()}")
            }
            get<PandaPath> { path ->
                result(path.age.toString())
            }
        }
    }.start(8080)
}