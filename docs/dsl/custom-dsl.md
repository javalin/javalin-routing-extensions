# Custom DSL

The DSL module is designed to be extensible. You can create custom DSL implementations tailored to your project's needs by implementing the `RoutingDslFactory` interface.

## Architecture

The DSL system consists of three main components:

1. **Factory** — creates configuration and handlers
2. **Configuration** — defines available route methods
3. **Scope** — provides the handler context (what's available inside route lambdas)

## Creating a Custom DSL

### Step 1: Define a Custom Scope

The scope determines what methods are available inside route handlers:

```kotlin
open class CustomScope(override val ctx: Context) : DefaultContextScope, Context by ctx {

    fun helloWorld(): String = "Hello, World!"

    fun currentUser(): User = ctx.attribute("user")!!

    fun endpoint(): HandlerEntry =
        ctx.attribute<HandlerEntry>("javalin-handler-entry")!!

}
```

### Step 2: Create the Factory

Implement `RoutingDslFactory` to connect everything:

```kotlin
object CustomDsl : RoutingDslFactory<
    CustomConfiguration,
    DslRoute<CustomScope, Unit>,
    CustomScope,
    Unit
> {
    class CustomConfiguration :
        DefaultContextScopeConfiguration<DslRoute<CustomScope, Unit>, CustomScope, Unit>()

    override fun createConfiguration(): CustomConfiguration =
        CustomConfiguration()

    override fun createHandler(route: DslRoute<CustomScope, Unit>): Handler =
        Handler { ctx -> route.handler(CustomScope(ctx)) }

    override fun createExceptionHandler(
        handler: DslExceptionHandler<CustomScope, Exception, Unit>
    ): ExceptionHandler<Exception> =
        ExceptionHandler { exception, ctx -> handler(CustomScope(ctx), exception) }
}
```

### Step 3: Use Your Custom DSL

```kotlin
Javalin.create { config ->
    config.routes(DslRouting(CustomDsl)) {
        get("/") {
            // `helloWorld()` comes from CustomScope
            result(helloWorld())
        }
        get("/me") {
            json(currentUser())
        }
    }
}.start(8080)
```

## DefaultContextScope

The built-in `DefaultContextScope` interface provides a foundation for custom scopes:

```kotlin
interface DefaultContextScope {
    val ctx: Context
}
```

Implement it and delegate `Context` methods using Kotlin's `by` delegation:

```kotlin
class MyScope(override val ctx: Context) : DefaultContextScope, Context by ctx {
    // your custom methods
}
```

## DefaultContextScopeConfiguration

The `DefaultContextScopeConfiguration` extends the base DSL configuration with type-safe path support:

```kotlin
open class DefaultContextScopeConfiguration<
    ROUTE : DslRoute<CONTEXT, RESPONSE>,
    CONTEXT : DefaultContextScope,
    RESPONSE : Any
> : RoutingDslConfiguration<ROUTE, CONTEXT, RESPONSE>() {

    inline fun <reified PATH : Any> get(
        crossinline handler: CONTEXT.(PATH) -> RESPONSE
    ) { /* ... */ }

    inline fun <reified PATH : Any> post(
        crossinline handler: CONTEXT.(PATH) -> RESPONSE
    ) { /* ... */ }

    // ... same for all HTTP methods
}
```

This is what enables the `get<PandaPath> { path -> ... }` syntax.

## Real-World Example

See the [Reposilite](https://github.com/dzikoysk/reposilite) project for a production example of custom DSL usage with Javalin routing extensions.
