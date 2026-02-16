# Versioning

The annotated routing module provides built-in support for API versioning through HTTP headers. Multiple versions of the same endpoint can coexist in the same class.

## Basic Usage

Use the `@Version` annotation to define versioned endpoints:

```java
@Endpoints("/api")
class UserEndpoints {

    @Version("1")
    @Get("/users/{name}")
    void getUserV1(Context ctx, @Param String name) {
        ctx.result("V1: " + name);
    }

    @Version("2")
    @Get("/users/{name}")
    void getUserV2(Context ctx, @Param String name) {
        ctx.json(Map.of("version", "2", "name", name));
    }

}
```

## Client Usage

Clients specify the desired version via an HTTP header. By default, the header is `X-API-Version`:

```
GET /api/users/john
X-API-Version: 2
```

If the client requests a version that doesn't exist for the endpoint, the server responds with `400 Bad Request`.

## Custom Version Header

Configure a custom header name during setup:

::: code-group

```kotlin [Kotlin]
config.routes(Annotated) {
    apiVersionHeader = "X-Custom-Version"
    registerEndpoints(UserEndpoints())
}
```

```java [Java]
AnnotatedRouting.install(config, routing -> {
    routing.apiVersionHeader = "X-Custom-Version";
    routing.registerEndpoints(new UserEndpoints());
});
```

:::

## How It Works

1. Routes with `@Version` are grouped by their HTTP method and path
2. A single handler is registered for each group
3. When a request arrives, the plugin reads the version header
4. The corresponding versioned handler is invoked
5. If the requested version is not available, a `400 Bad Request` response is returned

## Combining with @OpenApi

Version annotations work with the Javalin OpenAPI plugin:

```java
@OpenApi(
    path = "/api/users/{name}",
    methods = { GET },
    versions = { "default", "2" },
    responses = {
        @OpenApiResponse(
            status = "200",
            content = @OpenApiContent(from = UserDto.class)
        )
    }
)
@Version("2")
@Get("/users/{name}")
void getUserV2(Context ctx, @Param String name) { }
```
