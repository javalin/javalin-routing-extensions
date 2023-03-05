package io.javalin.community.routing.dsl.examples

import io.javalin.Javalin
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.Route.POST
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.dsl.DslContainer
import io.javalin.community.routing.dsl.examples.CustomDsl.CustomScope
import io.javalin.community.routing.dsl.routing
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi

// Some dependencies
class ExampleService {
    fun save(animal: String) = println("Saved animal: $animal")
}

// Utility representation of custom routing in your application
abstract class ExampleRouting : DslContainer<DslRoute<CustomScope, Unit>, CustomScope, Unit>

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

    private val defaultExceptionHandler = exceptionHandler(Exception::class) { regularException ->
        println("Exception: ${regularException.message}")
    }

    override fun routes() = setOf(findAnimalByName, saveAnimal)
    override fun exceptionHandlers() = setOf(defaultExceptionHandler)

}

fun main() {
    // prepare dependencies
    val exampleService = ExampleService()

    // setup & launch application
    Javalin
        .create { it.routing(CustomDsl, AnimalEndpoints(exampleService) /*, provide more classes with endpoints */) }
        .start(8080)
}