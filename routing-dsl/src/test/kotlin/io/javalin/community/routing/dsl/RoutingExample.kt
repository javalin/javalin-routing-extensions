package io.javalin.community.routing.dsl

import io.javalin.community.routing.RouteMethod.GET
import io.javalin.http.Context

// TODO - Port property-dsl to the new DSL api

// Custom context
class AppContext(val context: Context)

// Some dependencies
class ExampleFacade

// Endpoint (domain router)
class ExampleEndpoint(private val exampleFacade: ExampleFacade) : StandardRoutes<AppContext, Unit>() {

    private val routeA = route("/a", GET) { context.result("A") }

    private val routeB = route("/b", GET) { context.result("B") }

    override val routes = setOf(routeA, routeB)

}

fun main() {
//    Javalin.create { configuration ->
//        RoutingPlugin<StandardRoute<AppContext, Unit>> { ctx, route -> route.handler(AppContext(ctx)) }
//            .registerRoutes(ExampleEndpoint(ExampleFacade()))
//            .also { configuration.plugins.register(it) }
//    }
//    .start(8080)
}