# Javalin Routing Extensions Plugin [![CI](https://github.com/javalin/javalin-routing-extensions/actions/workflows/gradle.yml/badge.svg)](https://github.com/javalin/javalin-routing-extensions/actions/workflows/gradle.yml)

## ** Work in progress **

---

Javalin is very flexible and allows you to extend it in many ways. 
This repository contains a set of extensions for Javalin routing system following some of the most popular patterns.
Each approach has pros and cons, so you should choose the one that fits your needs the best.

1. [Installation](#installation)
2. [Usage](#usage)
    1. [Annotations](#annotations)
    2. [DSL](#dsl)
        1. [In-place](#in-place)
        2. [Properties](#properties)
    3. [Coroutines](#coroutines) 
    4. [Core](#core)

## Installation

Javalin Routing Extensions is currently under development and not yet available on Maven Central.
You can use the following repository to access the latest snapshot version from Snapshots repository

```kotlin
maven {
    url("https://maven.reposilite.com/snapshots")
}
```

Each module is distributed as a separate artifact:

```kotlin
dependencies {
    val javalinRoutingExtensions = "5.3.2-SNAPSHOT"
    // TODO
}
```

## Usage

### Annotations

```kotlin
```

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
                result("Called endpoint: ${endpointHandlerPath()}")
            }
            get("/") {
                // `helloWorld` comes from CustomScope class
                result("Hello ${helloWorld()}")
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
You can find base implementation of custom DSL definition here: [InPlaceExample.kt](https://github.com/javalin/javalin-routing-extensions/blob/main/routing-dsl/src/test/kotlin/io/javalin/community/routing/dsl/InPlaceExample.kt)

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
abstract class ExampleRouting : Routes<CustomScope, Unit>

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
    private val saveAnimal = route("/animal/{name}", POST) {
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
you check its source code here: 
[PropertyDslExample.kt](https://github.com/javalin/javalin-routing-extensions/blob/main/routing-dsl/src/test/kotlin/io/javalin/community/routing/dsl/PropertyDslExample.kt)

### Coroutines

```kotlin
```

### Core

The core module contains shared components for the other modules.
The most important functionality is the `RouteComparator` 
which is used to sort given set of routes in the correct order by associated route path. 
