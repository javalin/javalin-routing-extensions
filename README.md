<div align="center">
    <h1>Javalin Routing Extensions Plugin</h1>
    <p>
        <a href="https://github.com/javalin/javalin-routing-extensions/actions/workflows/gradle.yml">
            <img src="https://github.com/javalin/javalin-routing-extensions/actions/workflows/gradle.yml/badge.svg" alt="CI">
        </a>
        <a href="https://codecov.io/gh/dzikoysk/javalin-routing-extensions">
            <img src="https://codecov.io/gh/dzikoysk/javalin-routing-extensions/branch/main/graph/badge.svg?token=eqL206FW98" alt="Coverage">
        </a>
        <a href="https://maven.reposilite.com/#/snapshots/io/javalin/community/routing">
            <img src="https://maven.reposilite.com/api/badge/latest/snapshots/io/javalin/community/routing/javalin-routing-extensions?color=A97BFF&name=Snapshot" alt="Version / Snapshot">
        </a>
    </p>
</div>

Javalin is very flexible and allows you to extend it in many ways. 
This repository contains a set of extensions for Javalin routing system following some of the most popular patterns.
Each approach has pros and cons, so you should choose the one that fits your needs the best.

1. [Installation](#installation)
2. [Usage](#usage)
    1. [Annotated](#annotated)
    2. [DSL](#dsl)
        1. [In-place](#in-place)
        2. [Properties](#properties)
    3. [Coroutines](#coroutines) 
    4. [Core](#core)

## Installation

Javalin Routing Extensions is currently under development and not yet available on Maven Central.
You can use the following repository to access the latest snapshot version from Snapshots repository

```kotlin
repositories {
    maven("https://maven.reposilite.com/snapshots")
}
```

Each module is distributed as a separate artifact:

```kotlin
dependencies {
    val javalinRoutingExtensions = "5.3.2-alpha.1-SNAPSHOT"
    implementation("io.javalin.community.routing:routing-core:$javalinRoutingExtensions")
    implementation("io.javalin.community.routing:routing-annotated:$javalinRoutingExtensions")
    implementation("io.javalin.community.routing:routing-dsl:$javalinRoutingExtensions")
    implementation("io.javalin.community.routing:routing-coroutines:$javalinRoutingExtensions")
}
```

## Usage

This chapter provides short preview of each module.
For more details, please refer to the documentation or full example of each module.
First of all, not each module is available for Java users, take a look on the table below to check requirements:


| Module                                         | Languages    | Reflections                                              |
|------------------------------------------------|--------------|----------------------------------------------------------|
| [Annotated](#annotated)                        | Java, Kotlin | Yes _(as long as we won't provide annotation processor)_ |
| [DSL In-place](#dsl)<br>[DSL Properties](#dsl) | Kotlin       | Optional                                                 |
| [Coroutines](#coroutines)                      | Kotlin       | No                                                       |
| [Core](#core)                                  | Java, Kotlin | No                                                       |

### Annotated

This module provides set of annotations to simplify routing setup & basic http operations.
This is probably the most common approach to routing in Java world,
some people may even say that it's the only one.
Take a look on the example below to see how it looks like:

```java
// register endpoints with prefix
@Endpoints("/api")
static final class ExampleEndpoints {

    private final ExampleService exampleService;

    // pass dependencies required to handle requests
    public ExampleEndpoints(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    // describe http method and path with annotation
    @Post("/hello")
    // use parameters to extract data from request
    void saveExample(Context context, @Nullable @Header(AUTHORIZATION) String authorization, @Body ExampleDto entity) {
        if (authorization == null) {
            context.status(401);
            return;
        }
        context.result(Objects.toString(exampleService.saveExample(entity)));
    }

    // you can combine it with OpenApi plugin
    @OpenApi(
            path = "/hello/{name}",
            methods = { GET },
            summary = "Find example by name",
            pathParams = { @OpenApiParam(name = "name", description = "Name of example to find") },
            responses = { @OpenApiResponse(status = "200", description = "Example found", content = @OpenApiContent(from = ExampleDto.class)) }
    )
    @Get("/hello/{name}")
    void findExample(Context context, @Param String name) {
        context.result(exampleService.findExampleByName(name));
    }

}

public static void main(String[] args) {
    Javalin.create(config -> {
        // prepare dependencies - manually, or using DI framework
        ExampleEndpoints exampleEndpoints = new ExampleEndpoints(new ExampleService());

        // register endpoints
        AnnotatedRoutingPlugin routingPlugin = new AnnotatedRoutingPlugin();
        routingPlugin.registerEndpoints(exampleEndpoints);
        config.plugins.register(routingPlugin);
    }).start(7000);
}
```

Unfortunately, this approach requires some reflections under the hood to make work at this moment, 
but **we're working on annotation processor to remove this requirement.**

Another thing you can notice is that we're creating endpoint class instance using constructor, 
not built-in DI framework.
This is because in general we consider this as a good practice - 
not only because you're in full control over the execution flow, 
but also because it forces you to make concious decision about the scope of your dependencies & architecture.

If you don't really care about it and you're just looking for a tool that will get the job done,
you can use literally any DI framework you want that is available for Java/Kotlin.
We may recommend Dagger2, because it verifies your code at compile time,
so it's safer than heavy reflection-based alternatives.

Full example: [AnnotationsRoutingExample.kt](https://github.com/javalin/javalin-routing-extensions/blob/main/routing-annotations/routing-annotated/src/test/java/io/javalin/community/routing/annotations/example/AnnotatedRoutingExample.java)

### DSL

DSL provides extensible base for creating custom DSLs for routing.

#### In-place

By default,
this module provides basic implementation of Ktor-like routing for Kotlin apps
with support for type-safe paths:

```kotlin
@Path("/panda/{age}")
data class PandaPath(val age: Int)

fun main() {
    Javalin.create { config ->
        config.routing(CustomDsl) {
            before {
                // `endpointHandlerPath` comes from Context class
                result("Called endpoint: ${matchedPath()}")
            }
            get("/") {
                // `helloWorld` comes from CustomScope class
                result(helloWorld())
            }
            get<PandaPath> { path ->
                // support for type-safe paths
                result(path.age.toString())
            }
        }
    }.start(8080)
}
```

Because of the extensible nature of DSL, you may adjust it to your needs!
You can find base implementation of custom DSL definition here: [InPlaceExample.kt](https://github.com/javalin/javalin-routing-extensions/blob/main/routing-dsl/src/test/kotlin/io/javalin/community/routing/dsl/examples/InPlaceExample.kt)

#### Properties

Property based implementation of DSL allows you to easily define routes in multiple sources,
outside the main setup scope.
This approach is very similar to Spring Boot's `@RestController` annotation, 
but it also supports your custom type-safe DSL, and 
you're in full control of your execution flow.

```kotlin
// Some dependencies
class ExampleService {
    fun save(animal: String) = println("Saved animal: $animal")
}

// Utility representation of custom routing in your application
abstract class ExampleRouting : DslRoutes<DslRoute<CustomScope, Unit>, CustomScope, Unit>

// Endpoint (domain router)
class AnimalEndpoints(private val exampleService: ExampleService) : ExampleRouting() {

    @OpenApi(
        path = "/animal/{name}",
        methods = [HttpMethod.GET]
    )
    private val findAnimalByName = route("/animal/<name>", GET) {
        result(pathParam("name"))
    }

    @OpenApi(
        path = "/animal/{name}",
        methods = [HttpMethod.POST]
    )
    private val saveAnimal = route("/animal/<name>", POST) {
        exampleService.save(pathParam("name"))
    }

    override fun routes() = setOf(findAnimalByName, saveAnimal)

}

fun main() {
    // prepare dependencies
    val exampleService = ExampleService()

    // setup & launch application
    Javalin
        .create { it.routing(CustomDsl, AnimalEndpoints(exampleService) /*, provide more classes with endpoints */) }
        .start(8080)
}
```

This example is based on previous in-place example, 
you can check its source code here: 
[PropertyDslExample.kt](https://github.com/javalin/javalin-routing-extensions/blob/main/routing-dsl/src/test/kotlin/io/javalin/community/routing/dsl/examples/PropertyDslExample.kt)

### Coroutines

**Experimental**: This module is more like a reference on how to use coroutines with Javalin.
The production-readiness of this module is unknown, especially in complex scenarios. 

The coroutines module provides works like `DSL :: Properties` module,
but it uses coroutines to provide asynchronous execution of endpoints.


```kotlin
// Custom scope used by routing DSL
class CustomScope(val ctx: Context) : Context by ctx {
    // blocks thread using reactive `delay` function
    suspend fun nonBlockingDelay(message: String): String = delay(2000L).let { message }
}

// Utility class representing group of reactive routes
abstract class ExampleRoutes : ReactiveRoutes<ReactiveRoute<CustomScope, Unit>, CustomScope, Unit>()

// Endpoint (domain router)
class ExampleEndpoint(private val exampleService: ExampleService) : ExampleRoutes() {
    
    // you can use suspend functions in coroutines context 
    // and as long as they're truly reactive, they won't freeze it
    private val nonBlockingAsync = reactiveRoute("/async", GET) {
        result(nonBlockingDelay("Non-blocking Async"))
    }

    override fun routes() = setOf(nonBlockingAsync)

}
    
fun main() {
    // prepare dependencies
    val exampleService = ExampleService()

    // create coroutines servlet with single-threaded executor
    val coroutinesServlet = DefaultContextCoroutinesServlet(
        executorService = Executors.newSingleThreadExecutor(),
        contextFactory = { CustomScope(it) },
    )

    // setup Javalin with reactive routing
    Javalin
        .create { config ->
            config.reactiveRouting(coroutinesServlet, ExampleEndpoint(exampleService))
        }
        .events {
            it.serverStopping { coroutinesServlet.prepareShutdown() }
            it.serverStopped { coroutinesServlet.completeShutdown() }
        }
        .start("127.0.0.1", 8080)
}
```

Full example: [ReactiveRoutingExample.kt](https://github.com/javalin/javalin-routing-extensions/blob/main/routing-coroutines/src/test/kotlin/io/javalin/community/routing/examples/ReactiveRoutingExample.kt)

### Core

The core module contains shared components for the other modules.
The most important functionality is the `RouteComparator` 
which is used to sort given set of routes in the correct order by associated route path. 

### Other examples

* [Reposilite](https://github.com/dzikoysk/reposilite) - real world app using Javalin with property-based DSL routing
