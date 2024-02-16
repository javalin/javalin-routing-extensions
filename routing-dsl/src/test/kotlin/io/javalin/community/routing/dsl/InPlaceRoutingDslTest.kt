package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.Route
import io.javalin.community.routing.dsl.DslRouting.Companion.Dsl
import io.javalin.community.routing.dsl.defaults.Path
import io.javalin.community.routing.dsl.specification.TestSpecification
import io.javalin.testtools.JavalinTest
import kong.unirest.Unirest.get
import kong.unirest.Unirest.request
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.ThrowableAssert.ThrowingCallable
import org.junit.jupiter.api.Test

class InPlaceRoutingDslTest : TestSpecification() {

    @Test
    fun `each http dsl method is properly mapped to javalin handler`() = JavalinTest.test(
        // given: a javalin app with routes defined using the dsl
        Javalin.create { config ->
            config.router.mount(Dsl) {
                it.before("/before") { header("test", "before") }
                it.beforeMatched("/before-matched") { header("test", "before-matched") }
                it.get("/before-matched") {  }
                it.after("/after") { header("test", "after") }
                it.afterMatched("/after-matched") { header("test", "after-matched") }
                it.get("/after-matched") {}
                it.get("/throwing") { throw RuntimeException() }
                it.exception(Exception::class) { header("exception", it::class.java.name) }
                it.get("/get") { header("test", "get") }
                it.post("/post") { header("test", "post") }
                it.put("/put") { header("test", "put") }
                it.patch("/patch") { header("test", "patch") }
                it.delete("/delete") { header("test", "delete") }
                it.head("/head") { header("test", "head") }
                it.options("/options") { header("test", "options") }
            }
        },
        defaultConfig
    ) { _, client ->
        // when: a request is made to the http route
        Route.entries
            .asSequence()
            .filter { it.isHttpMethod }
            .map { it.name.lowercase() }
            .map { it to request(it, "${client.origin}/$it").asEmpty() }
            .forEach { (method, response) ->
                // then: the response is the same as the route name
                assertThat(response.status).isEqualTo(200)
                assertThat(response.headers.getFirst("test")).isEqualTo(method)
            }

        // when: a request is made to the before handler
        get("${client.origin}/before").asEmpty().also {
            // then: the response is the same as the route name
            assertThat(it.status).isEqualTo(404)
            assertThat(it.headers.getFirst("test")).isEqualTo("before")
        }

        // when: a request is made to the before matched handler
        get("${client.origin}/before-matched").asEmpty().also {
            // then: the response is the same as the route name
            assertThat(it.status).isEqualTo(200)
            assertThat(it.headers.getFirst("test")).isEqualTo("before-matched")
        }

        // when: a request is made to the after handler
        get("${client.origin}/after").asEmpty().also {
            // then: the response is the same as the route name
            assertThat(it.status).isEqualTo(404)
            assertThat(it.headers.getFirst("test")).isEqualTo("after")
        }

        // when: a request is made to the after matched handler
        get("${client.origin}/after-matched").asEmpty().also {
            // then: the response is the same as the route name
            assertThat(it.status).isEqualTo(200)
            assertThat(it.headers.getFirst("test")).isEqualTo("after-matched")
        }

        // when: a request is made to the throwing handler
        get("${client.origin}/throwing").asEmpty().also {
            // then: the response is handled by exception handler
            assertThat(it.headers.getFirst("exception")).isEqualTo(RuntimeException::class.java.name)
        }
    }

    @Path("/path")
    class ValidPath

    @Test
    fun `should properly handle class based route`() = JavalinTest.test(
        // given: a javalin app with routes defined using the dsl
        Javalin.create { config ->
            config.router.mount(Dsl) {
                it.get<ValidPath> { result("Panda") }
            }
        },
        defaultConfig
    ) { _, client ->
        // when: a request is made to the http route with a path parameter
        val response = get("${client.origin}/path").asString()

        // then: the response contains properly mapped path parameter
        assertThat(response.body).isEqualTo("Panda")
    }

    @Path("/path/{name}")
    class ValidPathWithParameter(val name: String)

    @Test
    fun `should properly handle class based route with parameter`() = JavalinTest.test(
        // given: a javalin app with routes defined using the dsl
        Javalin.create { config ->
            config.router.mount(Dsl) {
                it.get<ValidPathWithParameter> { result(it.name) }
            }
        },
        defaultConfig
    ) { _, client ->
        // when: a request is made to the http route with a path parameter
        val response = get("${client.origin}/path/panda").asString()

        // then: the response contains properly mapped path parameter
        assertThat(response.body).isEqualTo("panda")
    }

    class MissingAnnotationPath(val name: String)

    @Test
    fun `should throw exception when class based route is invalid`() {
        // given: a javalin app with routes defined using the dsl
        val app = ThrowingCallable {
            Javalin.create {
                it.router.mount(Dsl) {
                    // when: a route is defined with path without @Path annotation
                    it.get<MissingAnnotationPath> { }
                }
            }
        }

        // then: an exception is thrown
        assertThatThrownBy(app)
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("@Path annotation not found")
    }

    @Path("/path/{typo}")
    class InvalidParameterPath(val invalid: String)

    @Test
    fun `should throw exception when path parameter doesn't match constructor parameter`() {
        // given: a javalin app with routes defined using the dsl
        val app = ThrowingCallable {
            Javalin.create {
                it.router.mount(Dsl) {
                    // when: a route is defined with invalid @Path annotation
                    it.get<InvalidParameterPath> { }
                }
            }
        }

        // then: an exception is thrown
        assertThatThrownBy(app)
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Path parameter 'invalid' not found in")
    }

    @Path("/path")
    class ReifiedPath

    @Test
    fun `should properly map all reified variants`() = JavalinTest.test(
        // given: a javalin app with routes defined by all available reified methods
        Javalin.create { config ->
            config.router.mount(Dsl) {
                it.before<ReifiedPath> { result("Before ") }
                it.beforeMatched<ReifiedPath> { result(result() + "Before-Matched ") }
                it.get<ReifiedPath> { result(result() + "GET") }
                it.put<ReifiedPath> { result(result() + "PUT") }
                it.post<ReifiedPath> { result(result() + "POST") }
                it.patch<ReifiedPath> { result(result() + "PATCH") }
                it.delete<ReifiedPath> { result(result() + "DELETE") }
                it.head<ReifiedPath> { result(result() + "HEAD") }
                it.options<ReifiedPath> { result(result() + "OPTIONS") }
                it.afterMatched<ReifiedPath> { result(result() + " After-Matched") }
                it.after<ReifiedPath> {
                    result(result() + " After")
                    header("test", result()!!)
                }
            }
        },
        defaultConfig
    ) { _, client ->
        // when: a request is made to the http route
        Route.entries
            .asSequence()
            .filter { it.isHttpMethod }
            .map { it to request(it.name, "${client.origin}/path").asString() }
            .forEach { (method, response) ->
                // then: the response is the same as the route name
                assertThat(response.headers.getFirst("test")).isEqualTo("Before Before-Matched $method After-Matched After")
            }
    }

}