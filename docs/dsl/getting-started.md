# Getting Started

The DSL routing module provides a Kotlin-first approach to defining routes. It offers two styles: in-place DSL for inline route definitions, and property-based DSL for organizing routes in dedicated classes.

## Installation

Add the DSL module to your dependencies:

::: code-group

```kotlin [Gradle (Kotlin)]
dependencies {
    implementation("io.javalin.community.routing:routing-dsl:7.0.1")
}
```

```xml [Maven]
<dependency>
    <groupId>io.javalin.community.routing</groupId>
    <artifactId>routing-dsl</artifactId>
    <version>7.0.1</version>
</dependency>
```

:::

## Quick Example

### In-Place DSL

Define routes inline during Javalin configuration:

```kotlin
import io.javalin.community.routing.dsl.DslRouting.Companion.Dsl

Javalin.create { config ->
    config.routes(Dsl) {
        get("/hello") { result("Hello, World!") }
        post("/hello") { result("Created") }
    }
}.start(8080)
```

### Property-Based DSL

Organize routes in dedicated classes:

```kotlin
import io.javalin.community.routing.dsl.defaults.DefaultRouting
import io.javalin.community.routing.Route.*

class HelloRoutes : DefaultRouting() {
    val hello = route(GET, "/hello") { result("Hello, World!") }
    val create = route(POST, "/hello") { result("Created") }

    override fun routes() = setOf(hello, create)
}

Javalin.create { config ->
    config.routes(Dsl) {
        register(HelloRoutes())
    }
}.start(8080)
```

## Key Features

- Type-safe path parameters with `@Path` annotation
- In-place and property-based route definitions
- Custom DSL extensions for project-specific patterns
- Exception handlers
- Interceptors (before/after)
- No reflection required (except for `@Path` type-safe paths which use `kotlin-reflect`)

## Next Steps

- [In-Place DSL](./in-place) — define routes inline
- [Property-Based DSL](./property-based) — organize routes in classes
- [Type-Safe Paths](./type-safe-paths) — compile-time safe path parameters
- [Custom DSL](./custom-dsl) — create your own DSL
