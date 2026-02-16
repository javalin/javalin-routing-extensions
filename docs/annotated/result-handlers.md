# Result Handlers

Result handlers control how return values from endpoint methods are written to the HTTP response. By default, `String` values are handled — register custom handlers for other types.

## Default Handlers

The following types are handled out of the box:

| Return Type | Behavior |
|-------------|----------|
| `String` | Written via `ctx.result(value)` |
| `Unit` (Kotlin) | No action |
| `void` / `Void` | No action |

## Custom Result Handlers

Register handlers for custom return types:

::: code-group

```java [Java]
AnnotatedRouting.install(config, routing -> {
    routing.registerResultHandler(User.class, (ctx, user) -> {
        ctx.json(user);
    });
    routing.registerEndpoints(new UserEndpoints());
});
```

```kotlin [Kotlin]
config.routes(Annotated) {
    registerResultHandler<User> { ctx, user ->
        ctx.json(user)
    }
    registerEndpoints(UserEndpoints())
}
```

:::

Now endpoint methods can return `User` directly:

```java
@Get("/users/{id}")
User getUser(@Param int id) {
    return userService.findById(id);
}
```

## Multiple Result Handlers

Register handlers for different types:

```kotlin
config.routes(Annotated) {
    registerResultHandler<User> { ctx, user ->
        ctx.json(user)
    }
    registerResultHandler<ByteArray> { ctx, bytes ->
        ctx.result(bytes)
    }
    registerResultHandler<Map<*, *>> { ctx, map ->
        ctx.json(map)
    }
    registerEndpoints(UserEndpoints())
}
```

## HandlerResultConsumer

The `HandlerResultConsumer` functional interface accepts a `Context` and the return value:

```kotlin
fun interface HandlerResultConsumer<T> {
    fun handle(ctx: Context, value: T)
}
```
