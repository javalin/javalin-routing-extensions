# Overview

Javalin is a lightweight web library that is very flexible and allows you to extend it in many ways. This project provides a set of plugins for Javalin's routing system following some of the most popular patterns. Each approach has pros and cons, so you should choose the one that fits your needs best.

## Architecture

The project is organized into four modules:

```
routing-core                          # Shared interfaces and utilities
routing-annotations/
  ├── routing-annotated-specification # Annotation definitions
  └── routing-annotated              # Reflection-based implementation
routing-dsl                          # Kotlin DSL routing
routing-coroutines                   # Kotlin coroutines routing
```

All routing modules share a common registration pattern through `JavalinConfig.routes()`:

```kotlin
Javalin.create { config ->
    config.routes(RoutingModule) {
        // module-specific configuration
    }
}
```

## Choosing an Approach

### Annotated Routing

Annotations provide a familiar, declarative way to define endpoints with automatic parameter extraction.

```java
@Endpoints("/api")
class UserEndpoints {
    @Get("/users/{id}")
    void getUser(Context ctx, @Param int id) {
        ctx.result("User: " + id);
    }
}
```

**Pros:** Familiar to Java developers, works with Java and Kotlin, automatic parameter injection, built-in versioning.

**Cons:** Uses reflection, requires `-parameters` compiler flag for named parameters.

### DSL Routing

Best for Kotlin projects that want type-safe, concise route definitions. Two flavors available:

**In-place** — define routes inline during Javalin configuration:

```kotlin
config.routes(Dsl) {
    get("/users/{id}") { result(pathParam("id")) }
}
```

**Property-based** — organize routes in dedicated classes:

```kotlin
class UserRoutes : DefaultRouting() {
    val getUser = route(GET, "/users/{id}") { result(pathParam("id")) }
    override fun routes() = setOf(getUser)
}
```

**Pros:** Type-safe paths, extensible custom DSL, no reflection required, concise syntax.

**Cons:** Kotlin only.

### Coroutines Routing

Best for Kotlin projects that need non-blocking, async endpoint execution with coroutines.

```kotlin
// Define a base class for your routes
abstract class AppRoutes :
    SuspendedRoutes<SuspendedRoute<CustomScope, Unit>, CustomScope, Unit>()

class UserRoutes : AppRoutes() {
    val getUser = route("/users/{id}", GET) {
        result(fetchUserAsync(pathParam("id")))
    }
    override fun routes() = setOf(getUser)
}
```

**Pros:** Non-blocking I/O, suspend function support, configurable dispatchers.

**Cons:** Kotlin only, experimental status.

## Dependency Injection

None of the routing modules include a built-in DI container. You construct endpoint classes yourself, which gives you full control over the dependency graph:

```java
var userService = new UserService(database);
var userEndpoints = new UserEndpoints(userService);

AnnotatedRouting.install(config, routing -> {
    routing.registerEndpoints(userEndpoints);
});
```

This approach works with any DI library or no DI at all.

## Real-World Usage

[Reposilite](https://github.com/dzikoysk/reposilite) is a production application using Javalin with property-based DSL routing from this project.
