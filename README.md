# Javalin RFCs [![CI](https://github.com/reposilite-playground/javalin-rfcs/actions/workflows/gradle.yml/badge.svg)](https://github.com/reposilite-playground/javalin-rfcs/actions/workflows/gradle.yml)
Various experimental extensions to [Javalin 4.x](https://github.com/tipsy/javalin) used in [Reposilite 3.x](https://github.com/dzikoysk/reposilite). Provides basic support for Kotlin coroutines and async routes with a set of useful utilities.

```groovy
repositories {
    maven { url 'https://repo.panda-lang.org/releases' }
}

dependencies {
    val version = "4.0.22"
    implementation "com.reposilite.javalin-rfcs:javalin-context:$version"
    implementation "com.reposilite.javalin-rfcs:javalin-reactive-routing:$version"
}
```

Project also includes [panda-lang :: expressible](https://github.com/panda-lang/expressible) library as a dependency. It's mainly used to provide `Result<VALUE, ERROR>` type and associated utilities.

#### Reactive Routing

Experimental router plugin that supports generic route registration with custom context and multiple routes within the same endpoints. 

```kotlin
// Custom context
class AppContext(val context: Context)

// Some dependencies
class ExampleFacade

// Endpoint (domain router)
class ExampleEndpoint(private val exampleFacade: ExampleFacade) : AbstractRoutes<AppContext, Unit>() {

    private val sync = route("/sync", GET, async = false) { blockingDelay("Sync") }

    private val blockingAsync = route("/async-blocking", GET) { blockingDelay("Blocking Async") }

    private val nonBlockingAsync = route("/async", GET) { nonBlockingDelay("Non-blocking Async") }

    override val routes = setOf(sync, blockingAsync, nonBlockingAsync)

}

private suspend fun nonBlockingDelay(message: String): String = delay(100L).let { message }
@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun blockingDelay(message: String): String =  sleep(100L).let { message }

fun main() {
    val exampleEndpoint = ExampleEndpoint(ExampleFacade())
    val dispatcher = ExclusiveDispatcher(Executors.newCachedThreadPool())

    Javalin
        .create { config ->
            ReactiveRoutingPlugin<AppContext, Unit>(
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
```

[~ source: RoutingExample.kt](https://github.com/reposilite-playground/javalin-rfcs/blob/main/javalin-reactive-routing/src/test/kotlin/com/reposilite/web/routing/RoutingExample.kt)

#### Context

Provides set of utility methods in `io.javalin.http.Context` class.

#### OpenAPI

Reimplemented OpenAPI module:

* https://github.com/reposilite-playground/javalin-openapi

To enable annotation processor, Swagger or ReDoc you have to add extra dependencies from repository listed above. 


### Used by

* [Reposilite](https://github.com/dzikoysk/reposilite)
