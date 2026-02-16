# Exception Handling

Register exception handlers alongside your endpoints using the `@ExceptionHandler` annotation.

## Basic Usage

```java
@Endpoints("/api")
class UserEndpoints {

    @Get("/users/{id}")
    void getUser(Context ctx, @Param int id) {
        User user = userService.findById(id);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        ctx.json(user);
    }

    @ExceptionHandler(NotFoundException.class)
    void handleNotFound(NotFoundException e, Context ctx) {
        ctx.status(404).result(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    void handleGeneral(Exception e, Context ctx) {
        ctx.status(500).result("Internal error: " + e.getClass().getSimpleName());
    }
}
```

## Method Signature

Exception handler methods typically accept two parameters — the exception and the `Context` — but they support the full range of parameter annotations just like regular route handlers:

```java
@ExceptionHandler(IllegalArgumentException.class)
void handleBadRequest(IllegalArgumentException e, Context ctx) {
    ctx.status(400).json(Map.of("error", e.getMessage()));
}
```

Parameters can appear in any order:

```java
@ExceptionHandler(Exception.class)
void handle(Context ctx, Exception e) {
    ctx.status(500).result(e.getMessage());
}
```

## Multiple Handlers

You can register handlers for different exception types. More specific handlers take priority over general ones:

```java
@ExceptionHandler(ValidationException.class)
void handleValidation(ValidationException e, Context ctx) {
    ctx.status(422).json(e.getErrors());
}

@ExceptionHandler(AuthenticationException.class)
void handleAuth(AuthenticationException e, Context ctx) {
    ctx.status(401).result("Unauthorized");
}

@ExceptionHandler(Exception.class)
void handleFallback(Exception e, Context ctx) {
    ctx.status(500).result("Something went wrong");
}
```
