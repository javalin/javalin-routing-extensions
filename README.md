# Javalin RFCs [![CI](https://github.com/reposilite-playground/javalin-rfcs/actions/workflows/gradle.yml/badge.svg)](https://github.com/reposilite-playground/javalin-rfcs/actions/workflows/gradle.yml)
Various experimental extensions to [Javalin 4.x](https://github.com/tipsy/javalin) used in [Reposilite 3.x](https://github.com/dzikoysk/reposilite). Provides basic support for Kotlin coroutines and async routes with a set of useful utilities.

```groovy
repositories {
    maven { url 'https://repo.panda-lang.org/releases' }
}

dependencies {
    val version = "1.0.3"
    implementation "com.reposilite.javalin-rfcs:javalin-context:$version"
    implementation "com.reposilite.javalin-rfcs:javalin-reactive-routing:$version"
}
```

Project also includes [panda-lang :: expressible](https://github.com/panda-lang/expressible) library as a dependency. It's mainly used to provide `Result<VALUE, ERROR>` type and associated utilities.

#### Context

Provides utility methods in `io.javalin.http.Context` class:

```kotlin
Context.error(ErrorResponse)
Context.contentLength(Long)
Context.encoding(Charset)
Context.encoding(String)
Context.contentDisposition(String)
Context.contentType(ContentType)
Context.resultAttachment(Name, ContentType, ContentLength, InputStream)
```

Provides generic `ErrorResponse` that supports removal of exception based error handling within app:
```kotlin
ErrorResponse(Int httpCode, String message)
ErrorResponse(HttpCode httpCode, String message)
/* Methods */
errorResponse(HttpCode httpCode, String message) -> Result<*, ErrorResponse>
// [...]
```

Adds `ContentType` enum with list of [Mozilla :: Common Types](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types) and properties:
* Determining whether this type represents human-readable content
* Associated extension

#### Reactive Routing

Experimental router plugin that supports generic route registration with custom context and multiple routes within the same endpoints. 

```kotlin
// Custom context
class AppContext(val context: Context)

// Some dependencies
class ExampleFacade

// Endpoint (domain router)
class ExampleEndpoint(private val exampleFacade: ExampleFacade) : AbstractRoutes<AppContext> {

    private val async = route("/async", GET) { "Async" }

    private val sync = route("/sync", GET, async = false) { context.result("Sync") }

    override val routes = setOf(async, sync)

}

fun main() {
    val exampleLogger = LoggerFactory.getLogger("Example")
    val exampleFacade = ExampleFacade()
    val exampleEndpoint = ExampleEndpoint(exampleFacade)

    val sharedThreadPool = QueuedThreadPool(4)
    val dispatcher = DispatcherWithShutdown(sharedThreadPool.asCoroutineDispatcher())
    sharedThreadPool.start()

    Javalin
        .create { config ->
            config.server { Server(sharedThreadPool) }

            ReactiveRoutingPlugin<AppContext>(
                logger = { exampleLogger },
                dispatcher = dispatcher,
                syncHandler = { ctx, route -> route.handler(AppContext(ctx)) },
                asyncHandler = { ctx, route, result -> result.complete(route.handler(AppContext(ctx))) }
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

#### OpenAPI

Reimplemented OpenAPI module:

* https://github.com/reposilite-playground/javalin-openapi

To enable annotation processor, Swagger or ReDoc you have to add extra dependencies from repository listed above. 

### Used by

* [Reposilite](https://github.com/dzikoysk/reposilite)
