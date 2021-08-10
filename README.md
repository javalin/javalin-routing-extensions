# Javalin RFCs [![CI](https://github.com/reposilite-playground/javalin-rfcs/actions/workflows/gradle.yml/badge.svg)](https://github.com/reposilite-playground/javalin-rfcs/actions/workflows/gradle.yml)
Various experimental extensions to [Javalin 4.x](https://github.com/tipsy/javalin) used in [Reposilite 3.x](https://github.com/dzikoysk/reposilite)

```groovy
repositories {
    maven { url 'https://repo.panda-lang.org/releases' }
}

dependencies {
    val version = "1.0.0"
    implementation "com.reposilite.javalin-rfcs:javalin-context:$version"
    implementation "com.reposilite.javalin-rfcs:javalin-coroutines:$version"
    implementation "com.reposilite.javalin-rfcs:javalin-error:$version"
    implementation "com.reposilite.javalin-rfcs:javalin-mimetypes:$version"
    implementation "com.reposilite.javalin-rfcs:javalin-openapi:$version"
    implementation "com.reposilite.javalin-rfcs:javalin-routing:$version"
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

#### Coroutines

TODO: Coroutines support

#### Error

Provides generic `ErrorResponse` that supports removal of exception based error handling within app:
```kotlin
ErrorResponse(Int httpCode, String message)
ErrorResponse(HttpCode httpCode, String message)

/* Methods */

errorResponse(HttpCode httpCode, String message) -> Result<*, ErrorResponse>
// [...]
```

#### MimeTypes

Provides `ContentType` enum with list of [Mozilla :: Common Types](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types) and properties:
* Determining whether this type represents human readable content
* Associated extension

#### OpenAPI

Mirror of reimplemented OpenAPI module:

* https://github.com/reposilite-playground/javalin-openapi

To enable annotation processor, Swagger or ReDoc you have to add extra dependencies from repository listed above. 

#### Routing

Experimental router plugin that supports generic route registration with custom context and multiple routes within the same endpoints. 

```kotlin
/* General */

// Custom context
class AppContext(val context: Context)

// Custom route to skip redeclaration of custom context
class AppRoute(
    path: String,
    vararg methods: RouteMethod,
    handler: AppContext.() -> Unit
) : Route<AppContext>(path = path, methods = methods, handler = handler)

/* Example Domain */

// Some dependencies
class ExampleFacade

// Endpoint (domain router)
class ExampleEndpoint(private val exampleFacade: ExampleFacade) : Routes<AppContext> {

    private val index = AppRoute("/index", GET) { context.result("Index") }

    private val subIndex = AppRoute("/index/sub", GET) { context.result("Sub") }

    override val routes = setOf(index, subIndex)

}

/* Runner */

fun main() {
    val exampleFacade = ExampleFacade()
    val exampleEndpoint = ExampleEndpoint(exampleFacade)

    Javalin
        .create { config ->
            val routing = RoutingPlugin { AppContext(it) }
            routing.registerRoutes(exampleEndpoint)
            config.registerPlugin(routing)
        }
        .start()
}
```

[~ source: RoutingExample.kt](https://github.com/reposilite-playground/javalin-rfcs/blob/main/javalin-routing/src/test/kotlin/com/reposilite/web/routing/RoutingExample.kt)
