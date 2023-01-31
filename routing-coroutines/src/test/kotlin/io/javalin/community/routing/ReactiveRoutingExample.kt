package io.javalin.community.routing

import io.javalin.community.routing.coroutines.AsyncRoute
import io.javalin.community.routing.coroutines.AsyncRoutes
import io.javalin.community.routing.coroutines.CoroutinesServlet
import io.javalin.community.routing.coroutines.ExclusiveDispatcher
import io.javalin.community.routing.RouteMethod.GET
import io.javalin.Javalin
import io.javalin.http.Context
import kotlinx.coroutines.delay
import java.lang.Thread.sleep
import java.util.concurrent.Executors

// Custom context
class ExampleContext(val context: Context) {
    suspend fun nonBlockingDelay(message: String): String = delay(2000L).let { message }
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun blockingDelay(message: String): String = sleep(2000L).let { message }
}

// Some dependencies
class ExampleService

// Endpoint (domain router)
class ExampleEndpoint(private val exampleService: ExampleService) : AsyncRoutes<ExampleContext, String>() {

    private val sync = route("/sync", GET, async = false) { blockingDelay("Sync") }

    private val blockingAsync = route("/async-blocking", GET) { blockingDelay("Blocking Async") }

    private val nonBlockingAsync = route("/async", GET) { nonBlockingDelay("Non-blocking Async") }

    override val routes = setOf(sync, blockingAsync, nonBlockingAsync)

}

fun main() {
    val exampleEndpoint = ExampleEndpoint(ExampleService())

    val dispatcher = ExclusiveDispatcher(Executors.newCachedThreadPool())
    val coroutinesServlet = CoroutinesServlet<ExampleContext, String>(
        errorConsumer = { name, throwable -> println("$name: ${throwable.message}") },
        dispatcher = dispatcher,
        syncHandler = { ctx, route -> route.handler(ExampleContext(ctx)) },
        asyncHandler = { ctx, route, _ -> route.handler(ExampleContext(ctx)) },
        responseConsumer = { ctx, response -> ctx.result(response) }
    )

    Javalin
        .create { config ->
            RoutingPlugin<AsyncRoute<ExampleContext, String>> { ctx, route -> coroutinesServlet.handle(ctx, route) }
                .registerRoutes(exampleEndpoint)
                .let { config.plugins.register(it) }
        }
        .events {
            it.serverStopping { dispatcher.prepareShutdown() }
            it.serverStopped { dispatcher.completeShutdown() }
        }
        .start("127.0.0.1", 8080)
}