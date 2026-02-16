# Getting Started

The annotated routing module provides an annotation-driven approach to defining endpoints. It works with both Java and Kotlin.

## First Endpoint

Create a class annotated with `@Endpoints` and add methods with HTTP method annotations:

::: code-group

```java [Java]
import static io.javalin.community.routing.annotations.AnnotatedRouting.Annotated;

@Endpoints("/api")
public class UserEndpoints {

    @Get("/users")
    void getAllUsers(Context ctx) {
        ctx.result("All users");
    }

    @Get("/users/{id}")
    void getUserById(Context ctx, @Param int id) {
        ctx.result("User: " + id);
    }

    @Post("/users")
    void createUser(Context ctx, @Body UserDto user) {
        ctx.json(user);
    }
}
```

```kotlin [Kotlin]
import io.javalin.community.routing.annotations.AnnotatedRouting.Annotated

@Endpoints("/api")
class UserEndpoints {

    @Get("/users")
    fun getAllUsers(ctx: Context) {
        ctx.result("All users")
    }

    @Get("/users/{id}")
    fun getUserById(ctx: Context, @Param id: Int) {
        ctx.result("User: $id")
    }

    @Post("/users")
    fun createUser(ctx: Context, @Body user: UserDto) {
        ctx.json(user)
    }
}
```

:::

## Registration

Register your endpoints during Javalin configuration:

::: code-group

```java [Java]
Javalin.create(config -> {
    AnnotatedRouting.install(config, routing -> {
        routing.registerEndpoints(new UserEndpoints());
    });
}).start(8080);
```

```kotlin [Kotlin]
Javalin.create { config ->
    config.routes(Annotated) {
        registerEndpoints(UserEndpoints())
    }
}.start(8080)
```

:::

You can register multiple endpoint classes:

```java
routing.registerEndpoints(
    new UserEndpoints(userService),
    new ProductEndpoints(productService),
    new OrderEndpoints(orderService)
);
```

## Path Prefix

The `@Endpoints` annotation accepts an optional path prefix that is prepended to all routes in the class:

```java
@Endpoints("/api/v1")
class UserEndpoints {
    @Get("/users")  // actual path: /api/v1/users
    void getUsers(Context ctx) { }
}
```

## Constructor Injection

Endpoint classes are plain objects — pass dependencies through the constructor:

```java
@Endpoints("/api")
class UserEndpoints {

    private final UserService userService;

    public UserEndpoints(UserService userService) {
        this.userService = userService;
    }

    @Get("/users/{id}")
    void getUser(Context ctx, @Param int id) {
        ctx.json(userService.findById(id));
    }
}
```

This works with any DI library or manual construction.

## Full Example

```java
@Endpoints("/api")
static final class ExampleEndpoints {

    private final ExampleService exampleService;

    public ExampleEndpoints(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    @Before
    void beforeEach(Context ctx) {
        ctx.header("X-Example", "Example");
    }

    @Post("/hello")
    void saveExample(Context context,
                     @Header(AUTHORIZATION) Optional<String> authorization,
                     @Body ExampleDto entity) {
        if (authorization.isEmpty()) {
            context.status(401);
            return;
        }
        exampleService.saveExample(entity);
    }

    @Version("2")
    @Get("/hello/{name}")
    void findExampleV2(Context context, @Param String name) {
        context.result(exampleService.findExampleByName(name).name);
    }

    @ExceptionHandler(Exception.class)
    void defaultExceptionHandler(Exception e, Context ctx) {
        ctx.status(500).result("Error: " + e.getClass());
    }

    @Ws("/events")
    WsHandler websocketEvents() {
        return new WsHandler() {
            @Override
            public void onConnect(WsConnectContext ctx) {
                System.out.println("Connected: " + ctx.sessionId());
            }

            @Override
            public void onMessage(WsMessageContext ctx) {
                ctx.send("Echo: " + ctx.message());
            }
        };
    }
}
```

## Next Steps

- [HTTP Methods](./http-methods) — all available method annotations
- [Parameters](./parameters) — extract data from requests
- [Interceptors](./interceptors) — before/after handlers
- [Versioning](./versioning) — serve multiple API versions
