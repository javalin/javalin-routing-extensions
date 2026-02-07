package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.dsl.DslRouting.Companion.Dsl
import io.javalin.community.routing.dsl.defaults.DefaultRouting
import io.javalin.community.routing.dsl.specification.TestSpecification
import io.javalin.community.routing.routes
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PropertyRoutingDslTest : TestSpecification() {

    private class ValidTestEndpoints : DefaultRouting() {

        override fun routes() = setOf(
            route(GET, "/test") { result("test") },
            route(GET, "/throwing") { throw RuntimeException() }
        )

        override fun exceptionHandlers() = setOf(
            exceptionHandler(RuntimeException::class) { result(it::class.java.name) }
        )

    }

    @Test
    fun `should register valid route in javalin instance`() =
        JavalinTest.test(
            Javalin.create { cfg ->
                cfg.routes(Dsl) {
                    register(
                        ValidTestEndpoints()
                    )
                }
            },
            defaultConfig
        ) { _, client ->
           assertThat(client.get("/test").body?.string()).isEqualTo("test")
           assertThat(client.get("/throwing").body?.string()).isEqualTo(RuntimeException::class.java.name)
        }

}