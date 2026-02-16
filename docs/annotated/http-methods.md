# HTTP Methods

Each HTTP method has a corresponding annotation. All method annotations accept two parameters: `value` (the route path) and `async` (whether to handle the request asynchronously).

## Available Annotations

| Annotation | HTTP Method | Default Path |
|------------|-------------|--------------|
| `@Get` | GET | `""` |
| `@Post` | POST | `""` |
| `@Put` | PUT | `""` |
| `@Delete` | DELETE | `""` |
| `@Patch` | PATCH | `""` |
| `@Head` | HEAD | `""` |
| `@Options` | OPTIONS | `""` |

## Basic Usage

```java
@Endpoints("/api/users")
class UserEndpoints {

    @Get
    void list(Context ctx) {
        ctx.json(userService.findAll());
    }

    @Get("/{id}")
    void findById(Context ctx, @Param int id) {
        ctx.json(userService.findById(id));
    }

    @Post
    void create(Context ctx, @Body UserDto user) {
        ctx.json(userService.create(user));
    }

    @Put("/{id}")
    void update(Context ctx, @Param int id, @Body UserDto user) {
        ctx.json(userService.update(id, user));
    }

    @Delete("/{id}")
    void delete(Context ctx, @Param int id) {
        userService.delete(id);
        ctx.status(204);
    }

    @Patch("/{id}")
    void patch(Context ctx, @Param int id, @Body Map<String, Object> fields) {
        ctx.json(userService.patch(id, fields));
    }

    @Head("/{id}")
    void exists(Context ctx, @Param int id) {
        ctx.status(userService.exists(id) ? 200 : 404);
    }

    @Options
    void options(Context ctx) {
        ctx.header("Allow", "GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS");
    }
}
```

## Annotation Properties

### path (`value`)

The route path, appended to the `@Endpoints` prefix. Supports path parameters with `{param}` syntax:

```java
@Get("/users/{userId}/posts/{postId}")
void getUserPost(Context ctx, @Param int userId, @Param int postId) { }
```

### async

Set `async = true` to handle the request asynchronously using Javalin's `ctx.async()`:

```java
@Get(value = "/heavy-computation", async = true)
void compute(Context ctx) {
    // The plugin wraps this handler in ctx.async { ... } automatically
    ctx.result(heavyWork());
}
```

When `async = true`, the plugin wraps the handler invocation in `ctx.async { }`, which runs the handler on a separate thread pool without blocking Jetty's request threads.

## Return Values

Handler methods can return values instead of calling `ctx.result()` directly. The return value is processed by registered [result handlers](./result-handlers):

```java
@Get("/greeting")
String greeting() {
    return "Hello, World!";
}
```

By default, `String` return values are written to the response via `ctx.result()`. Register custom result handlers for other types.

## @OpenApi Compatibility

Annotated routing works with the [Javalin OpenAPI plugin](https://github.com/javalin/javalin-openapi). Add `@OpenApi` annotations alongside method annotations:

```java
@OpenApi(
    path = "/api/users/{id}",
    methods = { GET },
    summary = "Find user by ID",
    pathParams = { @OpenApiParam(name = "id", type = Integer.class) },
    responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "404", description = "User not found")
    }
)
@Get("/users/{id}")
void findById(Context ctx, @Param int id) {
    ctx.json(userService.findById(id));
}
```
