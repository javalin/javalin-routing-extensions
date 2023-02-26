package io.javalin.community.routing.annotations

import io.javalin.Javalin
import io.javalin.community.routing.Route
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.route
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinConfig
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.Plugin
import java.lang.UnsupportedOperationException

class AnnotatedRoutingPluginConfiguration {
    var apiVersionHeader: String = "X-API-Version"
}

class AnnotatedRoutingPlugin @JvmOverloads constructor(
    private val configuration: AnnotatedRoutingPluginConfiguration = AnnotatedRoutingPluginConfiguration()
) : Plugin {

    private val registeredRoutes = mutableListOf<DslRoute<Context, Unit>>()

    private data class RouteIdentifier(val route: Route, val path: String)

    override fun apply(app: Javalin) {
        registeredRoutes
            .sortRoutes()
            .groupBy { RouteIdentifier(it.method, it.path) }
            .map { (id, routes) ->
                id to when (routes.size) {
                    1 -> routes.first().let { Handler { ctx -> it.handler(ctx) } }
                    else -> createVersionedRoute(id, routes)
                }
            }
            .forEach { (id, handler) ->
                app.route(id.route, id.path, handler)
            }
    }

    private fun createVersionedRoute(id: RouteIdentifier, routes: List<DslRoute<Context, Unit>>): Handler {
        val versions = routes.map { it.version }
        check(versions.size == versions.toSet().size) { "Duplicated version found for the same route: ${id.route} ${id.path} (versions: $versions)" }

        return Handler { ctx ->
            val version = ctx.header(configuration.apiVersionHeader)

            routes.firstOrNull { it.version == version }
                ?.handler
                ?.invoke(ctx)
                ?: throw BadRequestResponse("This endpoint does not support the requested API version ($version).")
        }
    }

    fun registerPrecompiledEndpoints() {
        throw UnsupportedOperationException("Not implemented")
    }

    fun registerEndpoints(vararg endpoints: Any) {
        val detectedRoutes = endpoints.flatMap { ReflectiveEndpointLoader.loadRoutesFromEndpoint(it) }
        registeredRoutes.addAll(detectedRoutes)
    }

}

fun JavalinConfig.registerAnnotatedEndpoints(vararg endpoints: Any) {
    val plugin = AnnotatedRoutingPlugin()
    plugin.registerEndpoints(*endpoints)
    this.plugins.register(plugin)
}