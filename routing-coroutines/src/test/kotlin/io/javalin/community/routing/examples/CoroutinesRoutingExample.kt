package io.javalin.community.routing.examples

import io.javalin.Javalin
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.coroutines.Coroutines
import io.javalin.community.routing.coroutines.SuspendedRoute
import io.javalin.community.routing.coroutines.SuspendedRoutes
import io.javalin.community.routing.coroutines.servlet.DefaultContextCoroutinesServlet
import io.javalin.http.Context
import java.lang.Thread.sleep
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay

// Some dependencies
class ExampleService {
    val streamId = AtomicInteger(0)
}

// Custom scope used by routing DSL
class CustomScope(val ctx: Context) : Context by ctx {

    // blocks thread using reactive `delay` function
    suspend fun nonBlockingDelay(message: String): String = delay(2000L).let { message }

    // truly blocks thread using blocking JVM `sleep` function
    fun blockingDelay(message: String): String = sleep(2000L).let { message }

}

// Utility class representing group of reactive routes
abstract class ExampleRoutes : SuspendedRoutes<SuspendedRoute<CustomScope, Unit>, CustomScope, Unit>()

// Endpoint (domain router)
class ExampleEndpoint(private val exampleService: ExampleService) : ExampleRoutes() {

    // you can use suspend functions in coroutines context
    // and as long as they're truly reactive, they won't freeze it
    private val nonBlockingAsync = route("/async", GET) {
        result(nonBlockingDelay("Non-blocking Async"))
    }

    // using truly-blocking functions in coroutines context will freeze thread anyway
    private val blockingAsync = route("/async-blocking", GET) {
        result(blockingDelay("Blocking Async"))
    }

    // you can also use async = false, to run coroutine in sync context (runBlocking)
    private val sync = route("/sync", GET, async = false) {
        result(blockingDelay("Sync"))
    }

    // you can visit /stream in browser and see that despite single-threaded executor,
    // you can make multiple concurrent requests and each request is handled
    private val stream = route("/stream", GET) {
        val id = exampleService.streamId.incrementAndGet()

        while (true) {
            println("${Thread.currentThread().name} | $id")
            delay(1000L)
        }
    }

    override fun routes() = setOf(sync, blockingAsync, nonBlockingAsync, stream)

}

fun main() {
    // prepare dependencies
    val exampleService = ExampleService()

    // create coroutines servlet with single-threaded executor
    val coroutinesServlet = DefaultContextCoroutinesServlet(
        executorService = Executors.newSingleThreadExecutor(),
        contextFactory = { CustomScope(it) },
    )

    // setup Javalin with reactive routing
    Javalin
        .create { config ->
            config.router.mount(Coroutines(coroutinesServlet)) {
                it.routes(ExampleEndpoint(exampleService))
            }
        }
        .events {
            it.serverStopping { coroutinesServlet.prepareShutdown() }
            it.serverStopped { coroutinesServlet.completeShutdown() }
        }
        .start("127.0.0.1", 8080)
}