# Parameters

Parameter annotations let you extract data from incoming requests and inject it directly into handler method arguments. The plugin handles type conversion automatically using Javalin's built-in validators.

## Available Annotations

| Annotation | Source | Example |
|------------|--------|---------|
| `@Param` | Path parameter | `/users/{id}` |
| `@Query` | Query string | `?page=1` |
| `@Header` | HTTP header | `Authorization: Bearer ...` |
| `@Body` | Request body | JSON payload |
| `@Form` | Form field | Form submission |
| `@Cookie` | Cookie value | `session_id=abc` |

## @Param — Path Parameters

Extract values from URL path segments:

```java
@Get("/users/{id}")
void getUser(Context ctx, @Param int id) {
    ctx.result("User: " + id);
}
```

You can explicitly specify the parameter name if it differs from the argument name:

```java
@Get("/users/{userId}")
void getUser(Context ctx, @Param("userId") int id) {
    ctx.result("User: " + id);
}
```

::: tip
Without the explicit name, the plugin uses the Java parameter name from bytecode. This requires the `-parameters` compiler flag. See [Setup](../introduction/setup) for details.
:::

## @Query — Query Parameters

Extract values from the query string:

```java
@Get("/users")
void searchUsers(Context ctx,
                 @Query String search,
                 @Query("page") int pageNumber,
                 @Query int size) {
    // GET /users?search=john&page=1&size=20
}
```

## @Header — HTTP Headers

Extract values from request headers:

```java
@Post("/data")
void receiveData(Context ctx,
                 @Header("Authorization") String auth,
                 @Header("Content-Type") String contentType) {
    // process with header values
}
```

## @Body — Request Body

Deserialize the entire request body into an object:

```java
@Post("/users")
void createUser(Context ctx, @Body UserDto user) {
    userService.create(user);
    ctx.status(201);
}
```

The body is deserialized using Javalin's configured JSON mapper (e.g., Jackson, Gson).

## @Form — Form Parameters

Extract values from form submissions:

```java
@Post("/login")
void login(Context ctx,
           @Form String username,
           @Form String password) {
    authService.authenticate(username, password);
}
```

## @Cookie — Cookie Values

Extract values from request cookies:

```java
@Get("/profile")
void profile(Context ctx, @Cookie("session_id") String sessionId) {
    ctx.json(sessionService.getUser(sessionId));
}
```

## Context Parameter

You can always include `Context` as a parameter to access the full Javalin context:

```java
@Get("/users/{id}")
void getUser(Context ctx, @Param int id) {
    ctx.json(userService.findById(id));
}
```

The `Context` parameter can appear at any position in the parameter list.

## Endpoint Parameter

In interceptors, you can inject the matched `Endpoint` to inspect route metadata:

```kotlin
@BeforeMatched
fun beforeEachMatched(ctx: Context, endpoint: Endpoint) {
    println("Matched: ${endpoint.method()} ${endpoint.path()}")
}
```

For `@Before` interceptors (which run even without a match), use a nullable type:

```kotlin
@Before("/specific")
fun beforeSpecific(ctx: Context, endpoint: Endpoint?) {
    // endpoint is null if no route matched
}
```

## Optional Parameters

### Java — `Optional<T>`

Wrap parameters in `Optional` to handle missing values:

```java
@Get("/users")
void searchUsers(Context ctx,
                 @Query Optional<String> search,
                 @Query("page") Optional<Integer> page) {
    String query = search.orElse("");
    int pageNum = page.orElse(1);
}
```

### Kotlin — Nullable Types

Use nullable types for optional parameters:

```kotlin
@Get("/users")
fun searchUsers(ctx: Context,
                @Query search: String?,
                @Query page: Int?) {
    val query = search ?: ""
    val pageNum = page ?: 1
}
```

## Type Conversion

Parameters are automatically converted to the declared type using Javalin's type conversion system. Supported types include all primitives, their wrappers, `String`, and any type with a registered converter.

| Source | Conversion Method |
|--------|-------------------|
| `@Param` | `ctx.pathParamAsClass()` |
| `@Query` | `ctx.queryParamAsClass()` |
| `@Header` | `ctx.headerAsClass()` |
| `@Form` | `ctx.formParamAsClass()` |
| `@Cookie` | Javalin `Validation` |
| `@Body` | `ctx.bodyAsClass()` |

## Parameter Summary

| Annotation | Explicit Name | Optional (Java) | Nullable (Kotlin) | Type Conversion |
|------------|--------------|------------------|-------------------|-----------------|
| `@Param` | Yes | `Optional<T>` | `T?` | `pathParamAsClass` |
| `@Query` | Yes | `Optional<T>` | `T?` | `queryParamAsClass` |
| `@Header` | Yes | `Optional<T>` | `T?` | `headerAsClass` |
| `@Form` | Yes | `Optional<T>` | `T?` | `formParamAsClass` |
| `@Cookie` | Yes | `Optional<T>` | `T?` | `Validation` |
| `@Body` | No | No | No | `bodyAsClass` |
