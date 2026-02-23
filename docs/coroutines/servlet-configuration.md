# Servlet Configuration

The coroutines module uses a servlet-based architecture to manage coroutine execution. Configure the servlet to control thread pools, dispatchers, and shutdown behavior.

## DefaultContextCoroutinesServlet

The simplest way to get started is with `DefaultContextCoroutinesServlet`:

```kotlin
val coroutinesServlet = DefaultContextCoroutinesServlet(
    executorService = Executors.newCachedThreadPool(),
    contextFactory = { CustomScope(it) }
)
```

### Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `executorService` | `ExecutorService` | Thread pool for coroutine execution |
| `contextFactory` | `(Context) -> CONTEXT` | Creates scope instances from Javalin context |

### Thread Pool Options

```kotlin
// Cached thread pool — grows as needed (good default)
Executors.newCachedThreadPool()

// Fixed thread pool — bounded concurrency
Executors.newFixedThreadPool(10)

// Single thread — sequential execution, useful for testing
Executors.newSingleThreadExecutor()
```

## CoroutinesServlet

For advanced configuration, use `CoroutinesServlet` directly:

```kotlin
open class CoroutinesServlet<CONTEXT, RESPONSE : Any>(
    name: String = "javalin-reactive-routing",
    coroutinesEnabled: Boolean = true,
    dispatcher: CoroutineDispatcher,
    syncHandler: suspend (Context, SuspendedRoute<CONTEXT, RESPONSE>) -> RESPONSE,
    asyncHandler: suspend (Context, SuspendedRoute<CONTEXT, RESPONSE>,
                           CompletableFuture<RESPONSE>) -> RESPONSE,
    responseConsumer: (suspend (Context, RESPONSE) -> Unit)? = null,
    uncaughtExceptionConsumer: (CoroutineNameRepresentation, Throwable) -> Unit
)
```

### Parameters

| Parameter | Description |
|-----------|-------------|
| `name` | Name for the coroutine scope (used in debugging) |
| `coroutinesEnabled` | Enable/disable coroutine execution |
| `dispatcher` | Coroutine dispatcher for route execution |
| `syncHandler` | Handler for `async = false` routes (uses `runBlocking`) |
| `asyncHandler` | Handler for `async = true` routes (uses `ctx.future()`) |
| `responseConsumer` | Optional post-processing of responses |
| `uncaughtExceptionConsumer` | Handler for uncaught exceptions in coroutines |

## Graceful Shutdown

The servlet provides two-phase shutdown to ensure in-flight requests complete:

```kotlin
config.events.also {
    // Phase 1: Stop accepting new requests, switch to fallback pool
    it.serverStopping { coroutinesServlet.prepareShutdown() }

    // Phase 2: Wait for remaining requests and shut down
    it.serverStopped { coroutinesServlet.completeShutdown() }
}
```

### ExclusiveDispatcherWithShutdown

Internally, the servlet uses `ExclusiveDispatcherWithShutdown` which wraps the executor service:

- During normal operation, requests are dispatched to the main executor
- During `prepareShutdown()`, new requests use a fallback thread pool
- During `completeShutdown()`, all pools are shut down

## Coroutine Scope

The servlet creates a `JavalinCoroutineScope` with a `SupervisorJob`, meaning individual route failures don't cancel other routes:

```kotlin
class JavalinCoroutineScope(
    parentContext: CoroutineContext,
    exceptionHandler: CoroutineExceptionHandler
) : CoroutineScope
```

The scope combines the parent context, a `SupervisorJob` (for fault isolation), and the exception handler into a single coroutine context.

## Complete Example

```kotlin
class CustomScope(val ctx: Context) : Context by ctx {
    suspend fun nonBlockingDelay(message: String): String =
        delay(2000L).let { message }

    fun blockingDelay(message: String): String =
        Thread.sleep(2000L).let { message }

}

abstract class ExampleRoutes :
    SuspendedRoutes<SuspendedRoute<CustomScope, Unit>, CustomScope, Unit>()

class ExampleEndpoint(private val service: ExampleService) : ExampleRoutes() {

    private val async = route("/async", GET) {
        result(nonBlockingDelay("Non-blocking"))
    }

    private val sync = route("/sync", GET, async = false) {
        result(blockingDelay("Sync"))
    }

    override fun routes() = setOf(async, sync)

}

fun main() {
    val service = ExampleService()

    val coroutinesServlet = DefaultContextCoroutinesServlet(
        executorService = Executors.newSingleThreadExecutor(),
        contextFactory = { CustomScope(it) }
    )

    Javalin.create { config ->
        config.routes(Coroutines(coroutinesServlet)) {
            register(ExampleEndpoint(service))
        }
        config.events.also {
            it.serverStopping { coroutinesServlet.prepareShutdown() }
            it.serverStopped { coroutinesServlet.completeShutdown() }
        }
    }.start("127.0.0.1", 8080)
}
```
