package io.javalin.community.routing.annotations

import io.javalin.community.routing.Route
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.invokeAsSamWithReceiver
import io.javalin.community.routing.registerRoute
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinConfig
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.router.InternalRouter
import io.javalin.router.RoutingApiInitializer
import io.javalin.router.RoutingSetupScope

fun interface HandlerResultConsumer<T> {
    fun handle(ctx: Context, value: T)
}

class AnnotatedRoutingConfig {

    var apiVersionHeader: String = "X-API-Version"
    var resultHandlers: MutableMap<Class<*>, HandlerResultConsumer<*>> = mutableMapOf(
        String::class.java to HandlerResultConsumer<String?> { ctx, value -> value?.also { ctx.result(it) } },
        Unit::class.java to HandlerResultConsumer<Unit?> { _, _ -> },
        Void::class.java to HandlerResultConsumer<Void?> { _, _ -> },
        Void.TYPE to HandlerResultConsumer<Void?> { _, _ -> },
    )
    internal val registeredRoutes = mutableListOf<Any>()

    fun <T> registerResultHandler(type: Class<T>, handler: HandlerResultConsumer<T>): AnnotatedRoutingConfig = also {
        this.resultHandlers[type] = handler
    }

    inline fun <reified T> registerResultHandler(handler: HandlerResultConsumer<T>): AnnotatedRoutingConfig =
        registerResultHandler(T::class.java, handler)

    fun registerEndpoints(vararg endpoints: Any) {
        registeredRoutes.addAll(endpoints)
    }

}

object AnnotatedRouting : RoutingApiInitializer<AnnotatedRoutingConfig> {

    @JvmField val Annotated = this

    private data class RouteIdentifier(val route: Route, val path: String)

    override fun initialize(cfg: JavalinConfig, internalRouter: InternalRouter, setup: RoutingSetupScope<AnnotatedRoutingConfig>) {
        val configuration = AnnotatedRoutingConfig()
        setup.invokeAsSamWithReceiver(configuration)

        val loader = ReflectiveEndpointLoader(configuration.resultHandlers)
        val registeredRoutes = mutableListOf<AnnotatedRoute>()
        val registeredExceptionHandlers = mutableListOf<AnnotatedException>()

        configuration.registeredRoutes.forEach {
            val detectedRoutes = loader.loadRoutesFromEndpoint(it)
            registeredRoutes.addAll(detectedRoutes)

            val detectedExceptionHandlers = loader.loadExceptionHandlers(it)
            registeredExceptionHandlers.addAll(detectedExceptionHandlers)
        }

        registeredRoutes
            .sortRoutes()
            .groupBy { RouteIdentifier(it.method, it.path) }
            .map { (id, routes) ->
                id to when (routes.size) {
                    1 -> routes.first().let { Handler { ctx -> it.handler(ctx) } }
                    else -> createVersionedRoute(
                        apiVersionHeader = configuration.apiVersionHeader,
                        id = id,
                        routes = routes
                    )
                }
            }
            .forEach { (id, handler) ->
                internalRouter.registerRoute(id.route, id.path, handler)
            }

        registeredExceptionHandlers.forEach { annotatedException ->
            internalRouter.addHttpExceptionHandler(annotatedException.type.java) { exception, ctx ->
                annotatedException.handler.invoke(ctx, exception)
            }
        }
    }

    private fun createVersionedRoute(apiVersionHeader: String, id: RouteIdentifier, routes: List<DslRoute<Context, Unit>>): Handler {
        val versions = routes.map { it.version }
        check(versions.size == versions.toSet().size) { "Duplicated version found for the same route: ${id.route} ${id.path} (versions: $versions)" }

        return Handler { ctx ->
            val version = ctx.header(apiVersionHeader)

            routes.firstOrNull { it.version == version }
                ?.handler
                ?.invoke(ctx)
                ?: throw BadRequestResponse("This endpoint does not support the requested API version ($version).")
        }
    }

}