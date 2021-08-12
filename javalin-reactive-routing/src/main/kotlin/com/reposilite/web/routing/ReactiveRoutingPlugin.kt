package com.reposilite.web.routing

import com.reposilite.web.coroutines.JavalinCoroutineScope
import com.reposilite.web.http.response
import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.http.Context
import io.javalin.http.Handler
import io.ktor.server.engine.CoroutineNameRepresentation
import io.ktor.server.engine.DefaultUncaughtExceptionHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ReactiveRoutingPlugin<CONTEXT>(
    private val errorConsumer: (CoroutineNameRepresentation, Throwable) -> Unit,
    name: String = "javalin-rfc-scope:reactive-routing",
    parentContext: CoroutineContext = EmptyCoroutineContext,
    exceptionHandler: CoroutineExceptionHandler = DefaultUncaughtExceptionHandler(errorConsumer),
    private val coroutineName: CoroutineName = CoroutineName(name),
    private val dispatcher: CoroutineDispatcher,
    private val syncHandler: suspend (Context, Route<CONTEXT>) -> Any,
    private val asyncHandler: suspend (Context, Route<CONTEXT>, CompletableFuture<Any>) -> Any
) : Plugin, PluginLifecycleInit {

    private val routing: MutableSet<Route<CONTEXT>> = HashSet()
    private val scope = JavalinCoroutineScope(parentContext, exceptionHandler)

    override fun init(app: Javalin) { }

    override fun apply(app: Javalin) =
        routing
            .sortedWith(RouteComparator())
            .map { route -> Pair(route, createHandler(route)) }
            .forEach { (route, handler) ->
                route.methods.forEach { method ->
                    when (method) {
                        RouteMethod.HEAD -> app.head(route.path, handler)
                        RouteMethod.GET -> app.get(route.path, handler)
                        RouteMethod.PUT -> app.put(route.path, handler)
                        RouteMethod.POST -> app.post(route.path, handler)
                        RouteMethod.DELETE -> app.delete(route.path, handler)
                        RouteMethod.AFTER -> app.after(route.path, handler)
                        RouteMethod.BEFORE -> app.before(route.path, handler)
                    }
                }
            }

    private fun createHandler(route: Route<CONTEXT>): Handler =
        Handler { ctx ->
            if (route.async) {
                val result = CompletableFuture<Any>()

                scope.launch(dispatcher + coroutineName) {
                    result.complete(asyncHandler(ctx, route, result))
                }

                ctx.future(result) { response ->
                    response?.run { ctx.response(response) }
                }
            } else {
                runBlocking {
                    ctx.response(syncHandler(ctx, route))
                }
            }
        }

    fun registerRoutes(routes: Set<Route<CONTEXT>>): ReactiveRoutingPlugin<CONTEXT> =
        also { routing.addAll(routes) }

    fun registerRoutes(routes: Routes<CONTEXT>): ReactiveRoutingPlugin<CONTEXT> =
        also { routing.addAll(routes.routes) }

}