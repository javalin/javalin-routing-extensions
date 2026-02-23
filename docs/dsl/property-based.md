# Property-Based DSL

The property-based DSL lets you organize routes in dedicated classes with a type-safe Kotlin DSL.

## Basic Usage

Extend `DefaultRouting` and define routes as properties:

```kotlin
import io.javalin.community.routing.dsl.defaults.DefaultRouting
import io.javalin.community.routing.Route.*

class UserRoutes(private val userService: UserService) : DefaultRouting() {

    val findAll = route(GET, "/users") {
        json(userService.findAll())
    }

    val findById = route(GET, "/users/{id}") {
        json(userService.findById(pathParam("id").toInt()))
    }

    val create = route(POST, "/users") {
        val user = bodyAsClass<UserDto>()
        json(userService.create(user))
        status(201)
    }

    val delete = route(DELETE, "/users/{id}") {
        userService.delete(pathParam("id").toInt())
        status(204)
    }

    override fun routes() = setOf(findAll, findById, create, delete)

}
```

## Registration

Register route containers during Javalin configuration:

```kotlin
import io.javalin.community.routing.dsl.DslRouting.Companion.Dsl

val userService = UserService()
val productService = ProductService()

Javalin.create { config ->
    config.routes(Dsl) {
        register(UserRoutes(userService))
        register(ProductRoutes(productService))
    }
}.start(8080)
```

## Exception Handlers

Define exception handlers alongside routes:

```kotlin
class UserRoutes(private val userService: UserService) : DefaultRouting() {

    val findById = route(GET, "/users/{id}") {
        json(userService.findById(pathParam("id").toInt()))
    }

    val notFoundHandler = exceptionHandler(NotFoundException::class) { e ->
        status(404)
        result(e.message ?: "Not found")
    }

    val fallbackHandler = exceptionHandler(Exception::class) { e ->
        status(500)
        result("Error: ${e.message}")
    }

    override fun routes() = setOf(findById)
    override fun exceptionHandlers() = setOf(notFoundHandler, fallbackHandler)

}
```

## Constructor Injection

Route containers are regular Kotlin classes — pass dependencies through the constructor:

```kotlin
class AnimalEndpoints(private val animalService: AnimalService) : DefaultRouting() {

    val findByName = route(GET, "/animal/<name>") {
        result(pathParam("name"))
    }

    val save = route(POST, "/animal/<name>") {
        animalService.save(pathParam("name"))
    }

    override fun routes() = setOf(findByName, save)

}
```

## @OpenApi Compatibility

Property-based routes support `@OpenApi` annotations on the route properties:

```kotlin
class AnimalEndpoints(private val animalService: AnimalService) : DefaultRouting() {

    @OpenApi(
        path = "/animal/{name}",
        methods = [HttpMethod.GET]
    )
    val findByName = route(GET, "/animal/<name>") {
        result(pathParam("name"))
    }

    override fun routes() = setOf(findByName)

}
```

## DslContainer Interface

`DefaultRouting` is a convenient base class. Under the hood, it implements the `DslContainer` interface:

```kotlin
interface DslContainer<ROUTE : DslRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any>
    : Routes<ROUTE, CONTEXT, RESPONSE> {

    fun exceptionHandlers(): Collection<DslException<CONTEXT, Exception, RESPONSE>> =
        emptySet()

    fun route(method: Route, path: String,
              handler: CONTEXT.() -> RESPONSE): DslRoute<CONTEXT, RESPONSE> =
        DefaultDslRoute(method = method, path = path, handler = handler)

    fun <E : Exception> exceptionHandler(type: KClass<E>,
              handler: DslExceptionHandler<CONTEXT, E, RESPONSE>
    ): DslException<CONTEXT, Exception, RESPONSE> =
        DefaultDslException(type = type, handler = handler)
}
```

All three methods have default implementations, so you only need to override `routes()`. Implement this interface directly for more control over the route container behavior.
