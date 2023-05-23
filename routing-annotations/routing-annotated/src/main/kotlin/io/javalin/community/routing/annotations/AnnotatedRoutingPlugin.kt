package io.javalin.community.routing.annotations

import io.javalin.Javalin
import io.javalin.community.routing.Route
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.registerRoute
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinConfig
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.Plugin
import java.lang.UnsupportedOperationException

fun interface HandlerResultConsumer<T> {
    fun handle(ctx: Context, default: Any?)
}

class AnnotatedRoutingPluginConfiguration {
    var apiVersionHeader: String = "X-API-Version"
    var resultHandlers: MutableMap<Class<*>, HandlerResultConsumer<*>> = mutableMapOf()

    fun <T> registerResultHandler(type: Class<T>, handler: HandlerResultConsumer<T>): AnnotatedRoutingPluginConfiguration = also {
        this.resultHandlers[type] = handler
    }

    inline fun <reified T> registerResultHandler(handler: HandlerResultConsumer<T>): AnnotatedRoutingPluginConfiguration =
        registerResultHandler(T::class.java, handler)

}

class AnnotatedRoutingPlugin @JvmOverloads constructor(
    private val configuration: AnnotatedRoutingPluginConfiguration = AnnotatedRoutingPluginConfiguration()
) : Plugin {

    private val registeredRoutes = mutableListOf<AnnotatedRoute>()
    private val registeredExceptionHandlers = mutableListOf<AnnotatedException>()
    private val reflectiveEndpointLoader = ReflectiveEndpointLoader(configuration.resultHandlers)

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
                app.registerRoute(id.route, id.path, handler)
            }

        registeredExceptionHandlers.forEach { annotatedException ->
            app.exception(annotatedException.type.java) { exception, ctx ->
                annotatedException.handler.invoke(ctx, exception)
            }
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
        val detectedRoutes = endpoints.flatMap { reflectiveEndpointLoader.loadRoutesFromEndpoint(it) }
        registeredRoutes.addAll(detectedRoutes)

        val detectedExceptionHandlers = endpoints.flatMap { reflectiveEndpointLoader.loadExceptionHandlers(it) }
        registeredExceptionHandlers.addAll(detectedExceptionHandlers)
    }

}

fun JavalinConfig.registerAnnotatedEndpoints(configuration: AnnotatedRoutingPluginConfiguration, vararg endpoints: Any) {
    val plugin = AnnotatedRoutingPlugin(configuration)
    plugin.registerEndpoints(*endpoints)
    this.plugins.register(plugin)
}

fun JavalinConfig.registerAnnotatedEndpoints(vararg endpoints: Any) =
    registerAnnotatedEndpoints(AnnotatedRoutingPluginConfiguration(), *endpoints)