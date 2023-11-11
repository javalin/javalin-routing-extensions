package io.javalin.community.routing.dsl.examples

import io.javalin.Javalin
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.Route.POST
import io.javalin.community.routing.dsl.DslRouting.Companion.Dsl
import io.javalin.community.routing.dsl.defaults.DefaultRoutes
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi

// Some dependencies
private class ExampleService {
    fun save(animal: String) = println("Saved animal: $animal")
}

// Endpoint (domain router)
private class AnimalEndpoints(private val exampleService: ExampleService) : DefaultRoutes() {

    @OpenApi(
        path = "/animal/{name}",
        methods = [HttpMethod.GET]
    )
    private val findAnimalByName = route(GET, "/animal/<name>") {
        result(pathParam("name"))
    }

    @OpenApi(
        path = "/animal/{name}",
        methods = [HttpMethod.POST]
    )
    private val saveAnimal = route(POST, "/animal/<name>") {
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
    Javalin.createAndStart { cfg ->
        cfg.router.mount(Dsl) {
            it.routes(AnimalEndpoints(exampleService))
        }
    }
}