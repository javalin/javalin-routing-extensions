package com.reposilite.web.routing

import com.reposilite.web.http.response
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.Javalin
import io.javalin.http.Context

// Custom context
class AppContext(val context: Context)

// Some dependencies
class ExampleFacade

// Endpoint (domain router)
class ExampleEndpoint(private val exampleFacade: ExampleFacade) : AbstractRoutes<AppContext, Unit>() {

    private val routeA = route("/a", GET) { context.response("A") }

    private val routeB = route("/b", GET) { context.response("B") }

    override val routes = setOf(routeA, routeB)

}

fun main() {
    Javalin.create { configuration ->
        RoutingPlugin<AppContext, Unit> { ctx, route -> route.handler(AppContext(ctx)) }
            .registerRoutes(ExampleEndpoint(ExampleFacade()))
            .also { configuration.registerPlugin(it) }
    }
    .start(8080)
}