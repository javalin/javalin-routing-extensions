package com.reposilite.web.routing

import com.reposilite.web.coroutines.JavalinCoroutineScope
import com.reposilite.web.routing.RouteMethod.GET
import io.javalin.Javalin
import io.javalin.http.Context
import io.ktor.util.DispatcherWithShutdown
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.util.concurrent.CompletableFuture

// Custom context
class AppContext(val context: Context)

// Some dependencies
class ExampleFacade

// Endpoint (domain router)
class ExampleEndpoint(private val exampleFacade: ExampleFacade) : AbstractRoutes<AppContext>() {

    private val sync = route("/sync", GET, async = false) { context.result(blockingDelay("Sync")) }

    private val blockingAsync = route("/asyncBlocking", GET) { blockingDelay("Blocking Async") }

    private val nonBlockingAsync = route("/async", GET) { nonBlockingDelay("Non-blocking Async") }

    override val routes = setOf(sync, blockingAsync, nonBlockingAsync)

}

private suspend fun nonBlockingDelay(message: String): String =
    delay(100L).let { message }

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun blockingDelay(message: String): String =
    sleep(100L).let { message }

fun main() {
    val logger = LoggerFactory.getLogger("Example")

    val exampleFacade = ExampleFacade()
    val exampleEndpoint = ExampleEndpoint(exampleFacade)

    val sharedThreadPool = QueuedThreadPool(4)
    sharedThreadPool.name = "Javalin | Shared pool"
    sharedThreadPool.start()

    val dispatcher = DispatcherWithShutdown(sharedThreadPool.asCoroutineDispatcher())
    val coroutineName = CoroutineName("javalin-coroutine-handler")

    Javalin
        .create { config ->
            val javalinScope = JavalinCoroutineScope({ logger }, dispatcher)

            val routing = RoutingPlugin<AppContext>(
                { ctx, route ->
                    runBlocking {
                        route.handler(AppContext(ctx))
                    }
                },
                { ctx, route ->
                    val result = CompletableFuture<Any>()

                    javalinScope.launch(dispatcher + coroutineName) {
                        result.complete(route.handler(AppContext(ctx)))
                    }

                    ctx.future(result)
                }
            )

            routing.registerRoutes(exampleEndpoint)
            config.registerPlugin(routing)
            config.server { Server(sharedThreadPool) }
        }
        .events {
            it.serverStopping { dispatcher.prepareShutdown() }
            it.serverStopped { dispatcher.completeShutdown() }
        }
        .start("127.0.0.1", 8080)
}