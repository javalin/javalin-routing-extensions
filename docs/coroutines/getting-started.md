# Getting Started

::: warning Experimental
This module is experimental. It serves as a reference implementation for using coroutines with Javalin. Production-readiness in complex scenarios is not guaranteed.
:::

The coroutines module provides async and non-blocking endpoint execution using Kotlin coroutines. It follows the same property-based pattern as the DSL module but uses `suspend` lambdas.

## Installation

Add the coroutines module to your dependencies:

::: code-group

```kotlin [Gradle (Kotlin)]
dependencies {
    implementation("io.javalin.community.routing:routing-coroutines:7.0.0-beta.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
```

```xml [Maven]
<dependency>
    <groupId>io.javalin.community.routing</groupId>
    <artifactId>routing-coroutines</artifactId>
    <version>7.0.0-beta.1</version>
</dependency>
```

:::

## Quick Example

```kotlin
import io.javalin.community.routing.coroutines.Coroutines
import io.javalin.community.routing.coroutines.SuspendedRoute
import io.javalin.community.routing.coroutines.SuspendedRoutes
import io.javalin.community.routing.coroutines.servlet.DefaultContextCoroutinesServlet
import io.javalin.community.routing.Route.*

// 1. Define a custom scope
class CustomScope(val ctx: Context) : Context by ctx {
    suspend fun fetchAsync(message: String): String =
        delay(1000L).let { message }
}

// 2. Create a base class for your routes
abstract class AppRoutes :
    SuspendedRoutes<SuspendedRoute<CustomScope, Unit>, CustomScope, Unit>()

// 3. Define endpoints
class UserEndpoint : AppRoutes() {

    private val getUser = route("/users/{id}", GET) {
        result(fetchAsync("User: ${pathParam("id")}"))
    }

    override fun routes() = setOf(getUser)
}

// 4. Configure and start
fun main() {
    val coroutinesServlet = DefaultContextCoroutinesServlet(
        executorService = Executors.newCachedThreadPool(),
        contextFactory = { CustomScope(it) }
    )

    Javalin.create { config ->
        config.routes(Coroutines(coroutinesServlet)) {
            register(UserEndpoint())
        }
        config.events.also {
            it.serverStopping { coroutinesServlet.prepareShutdown() }
            it.serverStopped { coroutinesServlet.completeShutdown() }
        }
    }.start(8080)
}
```

## Key Concepts

### SuspendedRoute

Each route wraps a `suspend` lambda:

```kotlin
class SuspendedRoute<CONTEXT, RESPONSE : Any>(
    override val path: String,
    val method: Route,
    val async: Boolean = true,           // async by default
    val handler: suspend CONTEXT.() -> RESPONSE
) : Routed
```

### SuspendedRoutes

Base class for grouping routes:

```kotlin
abstract class SuspendedRoutes<
    ROUTE : SuspendedRoute<CONTEXT, RESPONSE>,
    CONTEXT,
    RESPONSE : Any
> : Routes<ROUTE, CONTEXT, RESPONSE> {

    fun route(
        path: String,
        method: Route,
        async: Boolean = true,
        handler: suspend CONTEXT.() -> RESPONSE
    ): SuspendedRoute<CONTEXT, RESPONSE>
}
```

### In-Place Route Registration

You can also register routes directly on the `CoroutinesRouting` configuration without creating a `SuspendedRoutes` subclass:

```kotlin
config.routes(Coroutines(coroutinesServlet)) {
    route(GET, "/hello") {
        result("Hello from coroutine!")
    }
    route(POST, "/data", async = false) {
        result("Sync response")
    }
}
```

### Graceful Shutdown

Always register shutdown hooks to ensure coroutines complete cleanly:

```kotlin
config.events.also {
    it.serverStopping { coroutinesServlet.prepareShutdown() }
    it.serverStopped { coroutinesServlet.completeShutdown() }
}
```

## Next Steps

- [Async vs Sync](./async-vs-sync) — understand execution modes
- [Servlet Configuration](./servlet-configuration) — configure dispatchers and thread pools
