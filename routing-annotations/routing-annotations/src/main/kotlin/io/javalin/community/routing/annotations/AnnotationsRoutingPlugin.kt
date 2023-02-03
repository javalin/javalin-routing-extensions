package io.javalin.community.routing.annotations

import io.javalin.Javalin
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.route
import io.javalin.community.routing.sortRoutes
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.Plugin

class AnnotationsRoutingPlugin : Plugin {

    private val routes = mutableListOf<DslRoute<Context, Unit>>()

    override fun apply(app: Javalin) {
        routes
            .sortRoutes()
            .map { it to Handler { ctx -> it.handler(ctx) } }
            .forEach { (route, handler) -> app.route(route.method, route.path, handler) }
    }

    fun registerPrecompiledEndpoints() {
        // todo
    }

    fun registerEndpoints(vararg endpoints: Any) {
        val detectedRoutes = endpoints.flatMap { ReflectiveEndpointLoader.loadRoutesFromEndpoint(it) }
        routes.addAll(detectedRoutes)
    }

}