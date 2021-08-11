package com.reposilite.web.routing

import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.Javalin
import io.javalin.http.Context

/* General */

// Custom context
class AppContext(val context: Context)

// Custom route to skip redeclaration of custom context
class AppRoute(
    path: String,
    vararg methods: RouteMethod,
    handler: AppContext.() -> Unit
) : Route<AppContext>(path = path, methods = methods, handler = handler)

/* Example Domain */

// Some dependencies
class ExampleFacade

// Endpoint (domain router)
class ExampleEndpoint(private val exampleFacade: ExampleFacade) : Routes<AppContext> {

    private val index = AppRoute("/index", GET) { context.result("Index") }

    private val subIndex = AppRoute("/index/sub", GET) { context.result("Sub") }

    override val routes = setOf(index, subIndex)

}

/* Runner */

fun main() {
    val exampleFacade = ExampleFacade()
    val exampleEndpoint = ExampleEndpoint(exampleFacade)

    Javalin
        .create { config ->
            val routing = RoutingPlugin<AppContext> { ctx, route -> route.handler(AppContext(ctx)) }
            routing.registerRoutes(exampleEndpoint)
            config.registerPlugin(routing)
        }
        .start()
}