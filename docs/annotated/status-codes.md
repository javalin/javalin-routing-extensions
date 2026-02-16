# Status Codes

The `@Status` annotation lets you declaratively set HTTP status codes for success and error responses on annotated endpoints.

## Basic Usage

```java
@Status(success = HttpStatus.CREATED)
@Post("/users")
void createUser(Context ctx, @Body UserDto user) {
    userService.create(user);
}
```

## Annotation Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `success` | `HttpStatus` | `UNKNOWN` | Status code for successful responses |
| `error` | `HttpStatus` | `UNKNOWN` | Status code for error responses |

When set to `UNKNOWN` (the default), the status code is not modified.

## Examples

### 201 Created

```java
@Status(success = HttpStatus.CREATED)
@Post("/users")
void createUser(Context ctx, @Body UserDto user) {
    ctx.json(userService.create(user));
}
```

### 204 No Content

```java
@Status(success = HttpStatus.NO_CONTENT)
@Delete("/users/{id}")
void deleteUser(Context ctx, @Param int id) {
    userService.delete(id);
}
```

### Success and Error

```java
@Status(success = HttpStatus.OK, error = HttpStatus.BAD_REQUEST)
@Put("/users/{id}")
void updateUser(Context ctx, @Param int id, @Body UserDto user) {
    userService.update(id, user);
}
```
