package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.RouteMethod.GET
import io.javalin.community.routing.dsl.specification.TestSpecification
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PropertyRoutingDslTest : TestSpecification() {

    private class ValidTestEndpoints : DefaultRoutes() {
        private val findRoute = route("/test", GET) { result("test") }
        override fun routes(): Collection<DefaultRoute> = setOf(findRoute)
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
        }

}