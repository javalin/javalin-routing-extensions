# Type-Safe Paths

The DSL module supports type-safe path parameters using the `@Path` annotation. Path parameters are automatically extracted, validated, and converted to their declared types at request time.

## Basic Usage

Define a data class annotated with `@Path`:

```kotlin
import io.javalin.community.routing.dsl.defaults.Path

@Path("/users/{id}")
data class UserPath(val id: Int)
```

Use it with the type-safe `get<T>` method:

```kotlin
config.routes(Dsl) {
    get<UserPath> { path ->
        result("User ID: ${path.id}")
    }
}
```

## Multiple Parameters

Path classes can have multiple parameters:

```kotlin
@Path("/users/{userId}/posts/{postId}")
data class UserPostPath(val userId: Int, val postId: Long)

config.routes(Dsl) {
    get<UserPostPath> { path ->
        result("User ${path.userId}, Post ${path.postId}")
    }
}
```

## How It Works

1. The `@Path` annotation defines the actual HTTP route
2. Primary constructor parameters are mapped to path variables by name
3. Parameters are automatically extracted from the request using Javalin's path parameter API
4. Type conversion happens automatically (e.g., `String` to `Int`)

## Type Conversion

Path parameters are converted to the declared Kotlin type. Supported types include:

- `String`
- `Int` / `Long` / `Short` / `Byte`
- `Float` / `Double`
- `Boolean`
- Any type supported by Javalin's type conversion system

## Example

```kotlin
@Path("/panda/{age}")
data class PandaPath(val age: Int)

Javalin.create { config ->
    config.routes(DslRouting(CustomDsl)) {
        before {
            result("Called endpoint: ${endpoint().path}")
        }
        get("/") {
            result(helloWorld())
        }
        get<PandaPath> { path ->
            result(path.age.toString())
        }
        exception(Exception::class) { anyException ->
            result(anyException.message ?: "Unknown error")
        }
    }
}.start(8080)
```
