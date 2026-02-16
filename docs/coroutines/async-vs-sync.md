# Async vs Sync

Each route in the coroutines module can run in async or sync mode, controlled by the `async` parameter.

## Async Mode (Default)

When `async = true` (the default), the route handler runs as a coroutine on the configured dispatcher. The request uses Javalin's `ctx.future()` mechanism, freeing the Jetty thread while the coroutine executes:

```kotlin
private val asyncRoute = route("/async", GET) {
    // This runs in a coroutine — truly non-blocking
    result(nonBlockingDelay("Async response"))
}
```

This is ideal for I/O-bound operations like database queries, HTTP calls, or file operations.

## Sync Mode

When `async = false`, the handler runs inside `runBlocking`, which blocks the calling thread until the coroutine completes:

```kotlin
private val syncRoute = route("/sync", GET, async = false) {
    // This blocks the thread
    result(blockingDelay("Sync response"))
}
```

Use sync mode when you need blocking behavior or when the handler calls APIs that aren't coroutine-friendly.

## Blocking vs Non-Blocking in Async Mode

Even in async mode, using truly blocking calls (like `Thread.sleep()` or blocking I/O) will freeze the coroutine's thread:

```kotlin
class CustomScope(val ctx: Context) : Context by ctx {

    // Truly non-blocking — uses coroutine delay
    suspend fun nonBlockingDelay(message: String): String =
        delay(2000L).let { message }

    // Blocks the thread despite being in a coroutine
    fun blockingDelay(message: String): String =
        Thread.sleep(2000L).let { message }

}

class ExampleEndpoint : AppRoutes() {

    // Non-blocking — frees the thread during delay
    private val nonBlocking = route("/non-blocking", GET) {
        result(nonBlockingDelay("Response"))
    }

    // Blocking — holds the thread during sleep
    private val blocking = route("/blocking", GET) {
        result(blockingDelay("Response"))
    }

    override fun routes() = setOf(nonBlocking, blocking)

}
```

## Concurrency Demonstration

With a single-threaded executor, non-blocking async routes can handle multiple concurrent requests because they yield the thread during suspension:

```kotlin
val coroutinesServlet = DefaultContextCoroutinesServlet(
    executorService = Executors.newSingleThreadExecutor(),
    contextFactory = { CustomScope(it) }
)
```

```kotlin
// This route handles multiple concurrent requests on a single thread
private val stream = route("/stream", GET) {
    val id = service.nextId()
    while (true) {
        println("${Thread.currentThread().name} | $id")
        delay(1000L) // yields the thread
    }
}
```

Each request gets its own coroutine, and `delay()` suspends without blocking, allowing other coroutines to run on the same thread.

## When to Use Each Mode

| Mode | Use When |
|------|----------|
| `async = true` | I/O-bound operations, suspend functions, non-blocking calls |
| `async = false` | Blocking APIs, legacy code, simple synchronous handlers |
