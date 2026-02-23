# Migration from 6.x

This guide covers all breaking changes when upgrading from Javalin Routing Extensions 6.x to 7.x.

## Requirements

| | 6.x | 7.x |
|---|-----|-----|
| Java | 11+ | 17+ |
| Kotlin | 1.9+ | 2.2+ |
| Javalin | 6.x | 7.x |

## Dependencies

Update your dependency version:

::: code-group
```kotlin [Gradle Kotlin DSL]
dependencies {
    implementation("io.javalin.community.routing:routing-annotated:7.0.0") // was 6.x
    // or
    implementation("io.javalin.community.routing:routing-dsl:7.0.0")
    // or
    implementation("io.javalin.community.routing:routing-coroutines:7.0.0")
}
```
```groovy [Gradle Groovy DSL]
dependencies {
    implementation 'io.javalin.community.routing:routing-annotated:7.0.0' // was 6.x
}
```
```xml [Maven]
<dependency>
    <groupId>io.javalin.community.routing</groupId>
    <artifactId>routing-annotated</artifactId>
    <version>7.0.0</version> <!-- was 6.x -->
</dependency>
```
:::

## Routing Configuration

The entry point for configuring routes has changed from `config.router.mount()` to `config.routes()`.

### Kotlin

```kotlin
// 6.x
Javalin.create { config ->
    config.router.mount(Annotated) { routing ->
        routing.registerEndpoints(MyEndpoints())
    }
}

// 7.x
Javalin.create { config ->
    config.routes(Annotated) { routing ->
        routing.registerEndpoints(MyEndpoints())
    }
}
```

### Java

For Java users, use the new `install()` helper method:

```java
// 6.x
Javalin.create(config -> {
    config.router.mount(Annotated, routing -> {
        routing.registerEndpoints(new MyEndpoints());
    });
});

// 7.x
Javalin.create(config -> {
    AnnotatedRouting.install(config, routing -> {
        routing.registerEndpoints(new MyEndpoints());
    });
});
```

## DSL Routing

### Registration Method

The `routes()` method on DSL configurations has been renamed to `register()`:

```kotlin
// 6.x
config.router.mount(DslRouting(CustomDsl)) {
    routes(AnimalEndpoints())
}

// 7.x
config.routes(DslRouting(CustomDsl)) {
    register(AnimalEndpoints())
}
```

### In-Place Routes

The `route()` helper internally uses `register()` now, but in-place route definitions (`get()`, `post()`, etc.) are unchanged:

```kotlin
// Works the same in both versions
config.routes(DslRouting(CustomDsl)) {
    get("/hello") {
        result("Hello!")
    }
}
```

## Coroutines Routing

The coroutines module follows the same `config.routes()` pattern:

```kotlin
// 6.x
config.router.mount(Coroutines(coroutinesServlet)) {
    routes(MyCoroutineEndpoints())
}

// 7.x
config.routes(Coroutines(coroutinesServlet)) {
    register(MyCoroutineEndpoints())
}
```

## Custom RoutingApiInitializer

If you implement `RoutingApiInitializer` directly, the interface signature has changed:

```kotlin
// 6.x
interface RoutingApiInitializer<SETUP> {
    fun initialize(
        cfg: JavalinConfig,
        internalRouter: InternalRouter,
        setup: RoutingSetupScope<SETUP>
    )
}

// 7.x
fun interface RoutingApiInitializer<SETUP> {
    fun initialize(
        state: JavalinState,
        setup: RoutingSetupScope<SETUP>
    )
}
```

Access the router and events through the `state` parameter:

```kotlin
// 6.x
override fun initialize(cfg: JavalinConfig, internalRouter: InternalRouter, setup: ...) {
    cfg.events.serverStarting { /* ... */ }
    internalRouter.registerRoute(route, path, handler)
}

// 7.x
override fun initialize(state: JavalinState, setup: ...) {
    state.events.serverStarting { /* ... */ }
    state.internalRouter.registerRoute(route, path, handler)
}
```

## Endpoint Roles → Metadata

Endpoint construction now uses `metadata` instead of `roles`:

```kotlin
// 6.x
Endpoint(method = HandlerType.GET, path = "/api", handler = handler, roles = roles)

// 7.x
Endpoint(
    method = HandlerType.GET,
    path = "/api",
    handler = handler,
    metadata =
        if (roles.isNotEmpty()) setOf(Roles(roles.toSet()))
        else emptySet()
)
```
