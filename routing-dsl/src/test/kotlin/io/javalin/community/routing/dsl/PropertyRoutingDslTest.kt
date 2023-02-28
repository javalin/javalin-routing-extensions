package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.dsl.defaults.DefaultRoutes
import io.javalin.community.routing.dsl.specification.TestSpecification
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PropertyRoutingDslTest : TestSpecification() {

    private class ValidTestEndpoints : DefaultRoutes() {

        override fun routes() = setOf(
            route("/test", GET) { result("test") },
            route("/throwing", GET) { throw RuntimeException() }
        )

        override fun exceptionHandlers() = setOf(
            exceptionHandler(RuntimeException::class) { result(it::class.java.name) }
        )

    }

    @Test
    fun `should register valid route in javalin instance`() =
        JavalinTest.test(
            Javalin.create {
                it.routing(ValidTestEndpoints())
            },
            defaultConfig
        ) { _, client ->
           assertThat(client.get("/test").body?.string()).isEqualTo("test")
           assertThat(client.get("/throwing").body?.string()).isEqualTo(RuntimeException::class.java.name)
        }

}