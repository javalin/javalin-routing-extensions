package com.reposilite.web.routing

import com.reposilite.web.coroutines.JavalinCoroutineScope
import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.http.Context
import io.javalin.http.Handler
import io.ktor.server.engine.DefaultUncaughtExceptionHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ReactiveRoutingPlugin<CONTEXT>(
    private val logger: () -> Logger,
    name: String = "javalin-rfc-scope:reactive-routing",
    parentContext: CoroutineContext = EmptyCoroutineContext,
    exceptionHandler: CoroutineExceptionHandler = DefaultUncaughtExceptionHandler(logger),
    private val coroutineName: CoroutineName = CoroutineName(name),
    private val dispatcher: CoroutineDispatcher,
    private val syncHandler: suspend (Context, Route<CONTEXT>) -> Unit,
    private val asyncHandler: suspend (Context, Route<CONTEXT>, CompletableFuture<Any>) -> Unit
) : Plugin, PluginLifecycleInit {

    private val routing: MutableSet<Route<CONTEXT>> = HashSet()
    private val scope = JavalinCoroutineScope(logger, parentContext, exceptionHandler)

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
        Handler {
            if (route.async) {
                val result = CompletableFuture<Any>()

                scope.launch(dispatcher + coroutineName) {
                    asyncHandler(it, route, result)
                }

                it.future(result)
            } else {
                runBlocking {
                    syncHandler(it, route)
                }
            }
        }

    fun registerRoutes(vararg routes: Set<Route<CONTEXT>>): ReactiveRoutingPlugin<CONTEXT> =
        also {
            routes.forEach { routing.addAll(it) }
        }

    fun registerRoutes(vararg routes: Routes<CONTEXT>): ReactiveRoutingPlugin<CONTEXT> =
        also {
            routes.forEach { routing.addAll(it.routes) }
        }

}