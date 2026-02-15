package io.javalin.community.routing.annotations

import io.javalin.community.routing.Route
import io.javalin.community.routing.RoutingApiInitializer
import io.javalin.community.routing.RoutingSetupScope
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.invokeAsSamWithReceiver
import io.javalin.community.routing.registerRoute
import io.javalin.community.routing.routes
import io.javalin.community.routing.sortRoutes
import io.javalin.config.JavalinConfig
import io.javalin.config.JavalinState
import io.javalin.event.JavalinLifecycleEvent
import io.javalin.event.JavalinLifecycleEvent.SERVER_STARTED
import io.javalin.event.JavalinLifecycleEvent.SERVER_STARTING
import io.javalin.event.JavalinLifecycleEvent.SERVER_START_FAILED
import io.javalin.event.JavalinLifecycleEvent.SERVER_STOPPED
import io.javalin.event.JavalinLifecycleEvent.SERVER_STOPPING
import io.javalin.event.JavalinLifecycleEvent.SERVER_STOP_FAILED
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.websocket.WsHandlerType

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

    /** For Java since the routes.mount(Annotated, setup -> ...) API has been removed from Javalin **/
    @JvmStatic
    fun install(config: JavalinConfig, setup: RoutingSetupScope<AnnotatedRoutingConfig>) {
        config.routes(Annotated, setup)
    }

    private data class RouteIdentifier(val route: Route, val path: String)

    override fun initialize(state: JavalinState, setup: RoutingSetupScope<AnnotatedRoutingConfig>) {
        val configuration = AnnotatedRoutingConfig()
        setup.invokeAsSamWithReceiver(configuration)

        val loader = ReflectiveEndpointLoader(
            internalRouter = state.internalRouter,
            resultHandlers = configuration.resultHandlers
        )

        val registeredEventListeners = mutableMapOf<JavalinLifecycleEvent, AnnotatedEvent>()
        val registeredRoutes = mutableListOf<AnnotatedRoute>()
        val registeredExceptionHandlers = mutableListOf<AnnotatedException>()
        val registeredWsRoutes = mutableListOf<AnnotatedWsRoute>()

        configuration.registeredRoutes.forEach {
            val detectedEventListeners = loader.loadEventHandlers(it)
            registeredEventListeners.putAll(detectedEventListeners)

            val detectedRoutes = loader.loadRoutesFromEndpoint(it)
            registeredRoutes.addAll(detectedRoutes)

            val detectedExceptionHandlers = loader.loadExceptionHandlers(it)
            registeredExceptionHandlers.addAll(detectedExceptionHandlers)

            val detectedWsRoutes = loader.loadWsHandlers(it)
            registeredWsRoutes.addAll(detectedWsRoutes)
        }

        registeredEventListeners.forEach { (key, event) ->
            when (key) {
                SERVER_STARTING -> state.events.serverStarting { event.invoke() }
                SERVER_STARTED -> state.events.serverStarted { event.invoke() }
                SERVER_START_FAILED -> state.events.serverStartFailed { event.invoke() }
                SERVER_STOP_FAILED -> state.events.serverStopFailed { event.invoke() }
                SERVER_STOPPING -> state.events.serverStopping { event.invoke() }
                SERVER_STOPPED -> state.events.serverStopped { event.invoke() }
            }
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
                state.internalRouter.registerRoute(id.route, id.path, handler)
            }

        registeredExceptionHandlers.forEach { annotatedException ->
            state.internalRouter.addHttpExceptionHandler(annotatedException.type.java) { exception, ctx ->
                annotatedException.handler.invoke(ctx, exception)
            }
        }

        registeredWsRoutes.forEach { wsRoute ->
            state.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET, wsRoute.path, wsRoute.wsConfig)
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