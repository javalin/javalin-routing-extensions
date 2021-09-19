package com.reposilite.web.routing

import com.reposilite.web.coroutines.JavalinCoroutineScope
import com.reposilite.web.coroutines.ktor.CoroutineNameRepresentation
import com.reposilite.web.coroutines.ktor.DefaultUncaughtExceptionHandler
import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.http.Context
import io.javalin.http.Handler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.EmptyCoroutineContext

class ReactiveRoutingPlugin<CONTEXT, RESPONSE : Any>(
    name: String = "javalin-rfc-scope:reactive-routing",
    private val coroutinesEnabled: Boolean = true,
    errorConsumer: (CoroutineNameRepresentation, Throwable) -> Unit,
    private val dispatcher: CoroutineDispatcher,
    private val syncHandler: suspend (Context, Route<CONTEXT, RESPONSE>) -> RESPONSE,
    private val asyncHandler: suspend (Context, Route<CONTEXT, RESPONSE>, CompletableFuture<RESPONSE>) -> RESPONSE
) : Plugin, PluginLifecycleInit {

    private val coroutineName: CoroutineName = CoroutineName(name)
    private val routing: MutableSet<Route<CONTEXT, RESPONSE>> = HashSet()
    private val exceptionHandler = DefaultUncaughtExceptionHandler(errorConsumer)
    private val scope = JavalinCoroutineScope(EmptyCoroutineContext, exceptionHandler)

    override fun init(app: Javalin) { }

    override fun apply(app: Javalin) =
        routing
            .sortedWith(RouteComparator())
            .map { route -> Pair(route, createHandler(route)) }
            .forEach { (route, handler) ->
                route.methods.forEach { method ->
                    when (method) {
                        RouteMethod.HEAD -> app.head(route.path, handler)
                        RouteMethod.PATCH -> app.patch(route.path, handler)
                        RouteMethod.OPTIONS -> app.options(route.path, handler)
                        RouteMethod.GET -> app.get(route.path, handler)
                        RouteMethod.PUT -> app.put(route.path, handler)
                        RouteMethod.POST -> app.post(route.path, handler)
                        RouteMethod.DELETE -> app.delete(route.path, handler)
                        RouteMethod.AFTER -> app.after(route.path, handler)
                        RouteMethod.BEFORE -> app.before(route.path, handler)
                    }
                }
            }

    private fun createHandler(route: Route<CONTEXT, RESPONSE>) =
        Handler { ctx ->
            if (coroutinesEnabled && route.async && ctx.handlerType().isHttpMethod()) {
                val result = CompletableFuture<RESPONSE>()
                ctx.future(result) { /* Disable default processing with empty body */ }

                scope.launch(dispatcher + coroutineName) {
                    runCatching {
                        asyncHandler(ctx, route, result)
                    }.onFailure {
                        result.completeExceptionally(it)
                    }
                }
            } else {
                runBlocking {
                    // Umm... I'm stuck step-coroutine, pls help me
                    // ~ https://github.com/Kotlin/kotlinx.coroutines/issues/1578
                    val parent = SupervisorJob(coroutineContext[Job])

                    val anyResponse = async(parent) {
                        syncHandler(ctx, route)
                    }

                    anyResponse.invokeOnCompletion { parent.complete() }
                    anyResponse.await()
                }
            }
        }

    fun registerRoutes(routes: Set<Route<CONTEXT, RESPONSE>>): ReactiveRoutingPlugin<CONTEXT, RESPONSE> =
        also { routing.addAll(routes) }

    fun registerRoutes(routes: Routes<CONTEXT, RESPONSE>): ReactiveRoutingPlugin<CONTEXT, RESPONSE> =
        also { routing.addAll(routes.routes) }

}