# Interceptors

Interceptors run before or after request handlers. They are useful for authentication, logging, CORS headers, and other cross-cutting concerns.

## Available Annotations

| Annotation | When It Runs | Default Path |
|------------|-------------|--------------|
| `@Before` | Before all requests | `*` (wildcard) |
| `@BeforeMatched` | Before matched routes only | `*` (wildcard) |
| `@After` | After all requests | `*` (wildcard) |
| `@AfterMatched` | After matched routes only | `*` (wildcard) |

## @Before

Runs before every request, even if no route matches:

```java
@Endpoints("/api")
class UserEndpoints {

    @Before
    void logRequest(Context ctx) {
        System.out.println("Request: " + ctx.method() + " " + ctx.path());
    }

    @Get("/users")
    void getUsers(Context ctx) {
        ctx.result("Users");
    }
}
```

## @BeforeMatched

Runs only when a route matches the request. Useful for authentication:

```java
@BeforeMatched
void authenticate(Context ctx) {
    if (ctx.header("Authorization") == null) {
        throw new UnauthorizedResponse();
    }
}
```

## @After

Runs after every request, regardless of whether a route matched:

```java
@After
void addCorsHeaders(Context ctx) {
    ctx.header("Access-Control-Allow-Origin", "*");
}
```

## @AfterMatched

Runs only after matched routes:

```java
@AfterMatched
void logResponse(Context ctx) {
    System.out.println("Response: " + ctx.status());
}
```

## Path Filtering

By default, interceptors apply to all paths (`*`). You can restrict them to specific paths:

```java
@Before("/admin/*")
void requireAdmin(Context ctx) {
    if (!isAdmin(ctx)) {
        throw new ForbiddenResponse();
    }
}
```

## Async Interceptors

Like HTTP method annotations, interceptors support `async = true`:

```java
@Before(value = "*", async = true)
void asyncBefore(Context ctx) {
    // async processing
}
```

## Ordering

Interceptors defined in the same `@Endpoints` class execute in declaration order. The plugin also detects potential conflicts when multiple `@Before` or `@After` handlers share the same path and warns you at startup.

## Example: Authentication + Logging

```java
@Endpoints("/api")
class SecuredEndpoints {

    @Before
    void logRequest(Context ctx) {
        ctx.attribute("requestStart", System.currentTimeMillis());
    }

    @BeforeMatched
    void authenticate(Context ctx) {
        String token = ctx.header("Authorization");
        if (token == null || !tokenService.isValid(token)) {
            throw new UnauthorizedResponse("Invalid token");
        }
    }

    @Get("/data")
    void getData(Context ctx) {
        ctx.json(dataService.getAll());
    }

    @AfterMatched
    void logDuration(Context ctx) {
        long start = ctx.attribute("requestStart");
        long duration = System.currentTimeMillis() - start;
        System.out.println("Request took: " + duration + "ms");
    }
}
```
