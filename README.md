# Javalin Routing Extensions Plugin [![CI](https://github.com/reposilite-playground/javalin-rfcs/actions/workflows/gradle.yml/badge.svg)](https://github.com/reposilite-playground/javalin-rfcs/actions/workflows/gradle.yml)
Various experimental extensions to [Javalin 5.x](https://github.com/tipsy/javalin) used in [Reposilite 3.x](https://github.com/dzikoysk/reposilite). Provides basic support for Kotlin coroutines and async routes with a set of useful utilities.

```groovy
repositories {
    maven { url 'https://maven.reposilite.com/releases' }
}

dependencies {
    val version = "5.0.0-SNAPSHOT"
    implementation "com.reposilite.javalin-rfcs:javalin-routing:$version"
    implementation "com.reposilite.javalin-rfcs:javalin-coroutines:$version"
}
```

Project also includes [panda-lang :: expressible](https://github.com/panda-lang/expressible) library as a dependency. It's mainly used to provide `Result<VALUE, ERROR>` type and associated utilities.

#### Routing

Experimental router plugin that supports generic route registration with custom context and multiple routes within the same endpoints. 

```kotlin
// Custom context
class AppContext(val context: Context)

// Some dependencies
class ExampleFacade

// Endpoint (domain router)
class ExampleEndpoint(private val exampleFacade: ExampleFacade) : StandardRoutes<AppContext, Unit>() {

    private val routeA = route("/a", GET) { context.response("A") }

    private val routeB = route("/b", GET) { context.response("B") }

    override val routes = setOf(routeA, routeB)

}

fun main() {
    Javalin.create { configuration ->
        RoutingPlugin<StandardRoute<AppContext, Unit>> { ctx, route -> route.handler(AppContext(ctx)) }
            .registerRoutes(ExampleEndpoint(ExampleFacade()))
            .also { configuration.plugins.register(it) }
    }
        .start(8080)
}
```

[~ source: RoutingExample.kt](https://github.com/reposilite-playground/javalin-rfcs/blob/main/javalin-reactive-routing/src/test/kotlin/com/reposilite/web/routing/RoutingExample.kt)

#### Coroutines

```kotlin
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
        responseConsumer = { ctx, response -> ctx.contextResolver().defaultFutureCallback(ctx, response) }
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
```

### Used by

* [Reposilite](https://github.com/dzikoysk/reposilite)
