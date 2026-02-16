# In-Place DSL

The in-place DSL lets you define routes directly in the Javalin configuration block.

## Basic Usage

```kotlin
import io.javalin.community.routing.dsl.DslRouting.Companion.Dsl

Javalin.create { config ->
    config.routes(Dsl) {
        get("/users") { result("All users") }
        post("/users") { result("User created") }
        put("/users/{id}") { result("User updated") }
        delete("/users/{id}") { result("User deleted") }
    }
}.start(8080)
```

## Available Methods

All HTTP methods and interceptors are available:

```kotlin
config.routes(Dsl) {
    // HTTP methods
    get("/path") { /* handler */ }
    post("/path") { /* handler */ }
    put("/path") { /* handler */ }
    delete("/path") { /* handler */ }
    patch("/path") { /* handler */ }
    head("/path") { /* handler */ }
    options("/path") { /* handler */ }

    // Interceptors
    before { /* runs before all requests */ }
    before("/admin/*") { /* runs before matching requests */ }
    beforeMatched { /* runs before matched routes */ }
    after { /* runs after all requests */ }
    afterMatched { /* runs after matched routes */ }

    // Exception handlers
    exception(Exception::class) { e ->
        result("Error: ${e.message}")
    }
}
```

## Handler Context

Inside route handlers, you have direct access to all `Context` methods via Kotlin delegation:

```kotlin
get("/users/{id}") {
    val id = pathParam("id")        // path parameters
    val page = queryParam("page")   // query parameters
    val auth = header("Authorization") // headers
    result("User: $id")             // set response
    status(200)                     // set status
    json(mapOf("id" to id))         // JSON response
}
```

## Exception Handlers

Register exception handlers that catch errors thrown by route handlers:

```kotlin
config.routes(Dsl) {
    get("/fail") {
        throw IllegalArgumentException("Bad input")
    }

    exception(IllegalArgumentException::class) { e ->
        status(400)
        result("Bad request: ${e.message}")
    }

    exception(Exception::class) { e ->
        status(500)
        result("Internal error: ${e.message}")
    }
}
```

## Type-Safe Paths

Combine in-place DSL with type-safe paths using `@Path`:

```kotlin
@Path("/users/{id}")
data class UserPath(val id: Int)

config.routes(Dsl) {
    get<UserPath> { path ->
        result("User: ${path.id}")
    }
}
```

See [Type-Safe Paths](./type-safe-paths) for details.

## Combining with Property-Based Routes

You can mix in-place routes with registered route containers:

```kotlin
config.routes(Dsl) {
    // In-place routes
    get("/health") { result("OK") }

    // Property-based routes
    register(UserRoutes(userService))
    register(ProductRoutes(productService))
}
```
