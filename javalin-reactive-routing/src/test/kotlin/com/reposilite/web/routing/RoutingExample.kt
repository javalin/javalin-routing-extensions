package com.reposilite.web.routing

import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.Javalin
import io.javalin.http.Context
import io.ktor.util.DispatcherWithShutdown
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.lang.Thread.sleep

// Custom context
class AppContext(val context: Context)

// Some dependencies
class ExampleFacade

// Endpoint (domain router)
class ExampleEndpoint(private val exampleFacade: ExampleFacade) : AbstractRoutes<AppContext>() {

    private val sync = route("/sync", GET, async = false) { blockingDelay("Sync") }

    private val blockingAsync = route("/async-blocking", GET) { blockingDelay("Blocking Async") }

    private val nonBlockingAsync = route("/async", GET) { nonBlockingDelay("Non-blocking Async") }

    override val routes = setOf(sync, blockingAsync, nonBlockingAsync)

}

private suspend fun nonBlockingDelay(message: String): String = delay(100L).let { message }
@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun blockingDelay(message: String): String =  sleep(100L).let { message }

fun main() {
    val exampleFacade = ExampleFacade()
    val exampleEndpoint = ExampleEndpoint(exampleFacade)

    val sharedThreadPool = QueuedThreadPool(4)
    val dispatcher = DispatcherWithShutdown(sharedThreadPool.asCoroutineDispatcher())
    sharedThreadPool.start()

    Javalin
        .create { config ->
            config.server { Server(sharedThreadPool) }

            ReactiveRoutingPlugin<AppContext>(
                errorConsumer = { name, throwable -> println("$name: ${throwable.message}") },
                dispatcher = dispatcher,
                syncHandler = { ctx, route -> route.handler(AppContext(ctx)) },
                asyncHandler = { ctx, route, _ -> route.handler(AppContext(ctx)) }
            )
            .registerRoutes(exampleEndpoint)
            .let { config.registerPlugin(it) }
        }
        .events {
            it.serverStopping { dispatcher.prepareShutdown() }
            it.serverStopped { dispatcher.completeShutdown() }
        }
        .start("127.0.0.1", 8080)
}