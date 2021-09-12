package com.reposilite.web.routing

import com.reposilite.web.coroutines.ktor.DispatcherWithShutdown
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.Javalin
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.lang.Thread.sleep

// Endpoint (domain router)
class SharedExampleEndpoint() : AbstractRoutes<AppContext, Unit>() {

    private val sync = route("/sync", GET, async = false) { blockingDelay("Sync") }

    private val blockingAsync = route("/async-blocking", GET) { blockingDelay("Blocking Async") }

    private val nonBlockingAsync = route("/async", GET) { nonBlockingDelay("Non-blocking Async") }

    override val routes = setOf(sync, blockingAsync, nonBlockingAsync)

}

private suspend fun nonBlockingDelay(message: String): String = delay(100L).let { message }
@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun blockingDelay(message: String): String =  sleep(100L).let { message }

fun main() {
    val sharedThreadPool = QueuedThreadPool(4)
    val dispatcher = DispatcherWithShutdown(sharedThreadPool.asCoroutineDispatcher())
    sharedThreadPool.start()

    Javalin
        .create { config ->
            config.server { Server(sharedThreadPool) }

            ReactiveRoutingPlugin<AppContext, Unit>(
                errorConsumer = { name, throwable -> println("$name: ${throwable.message}") },
                dispatcher = dispatcher,
                syncHandler = { ctx, route -> route.handler(AppContext(ctx)) },
                asyncHandler = { ctx, route, _ -> route.handler(AppContext(ctx)) }
            )
            .registerRoutes(SharedExampleEndpoint())
            .let { config.registerPlugin(it) }
        }
        .events {
            it.serverStopping { dispatcher.prepareShutdown() }
            it.serverStopped { dispatcher.completeShutdown() }
        }
        .start("127.0.0.1", 8080)
}