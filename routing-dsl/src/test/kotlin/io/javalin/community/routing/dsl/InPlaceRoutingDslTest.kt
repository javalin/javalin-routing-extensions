package io.javalin.community.routing.dsl

import io.javalin.Javalin
import io.javalin.community.routing.Route
import io.javalin.community.routing.dsl.DslRouting.Companion.Dsl
import io.javalin.community.routing.dsl.defaults.DefaultDsl
import io.javalin.community.routing.dsl.defaults.Path
import io.javalin.community.routing.dsl.specification.TestSpecification
import io.javalin.community.routing.routes
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
            config.routes(Dsl) {
                before("/before") { header("test", "before") }
                beforeMatched("/before-matched") { header("test", "before-matched") }
                get("/before-matched") {  }
                after("/after") { header("test", "after") }
                afterMatched("/after-matched") { header("test", "after-matched") }
                get("/after-matched") {}
                get("/throwing") { throw RuntimeException() }
                exception(Exception::class) { header("exception", it::class.java.name) }
                get("/get") { header("test", "get") }
                post("/post") { header("test", "post") }
                put("/put") { header("test", "put") }
                patch("/patch") { header("test", "patch") }
                delete("/delete") { header("test", "delete") }
                head("/head") { header("test", "head") }
                options("/options") { header("test", "options") }
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
            config.routes(Dsl) {
                get<ValidPath> { result("Panda") }
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
            config.routes(Dsl) {
                get<ValidPathWithParameter> { result(it.name) }
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
                it.routes(Dsl) {
                    // when: a route is defined with path without @Path annotation
                    get<MissingAnnotationPath> { }
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
                it.routes(Dsl) {
                    // when: a route is defined with invalid @Path annotation
                    get<InvalidParameterPath> { }
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
            config.routes(Dsl) {
                before<ReifiedPath> { result("Before ") }
                beforeMatched<ReifiedPath> { result(result() + "Before-Matched ") }
                get<ReifiedPath> { result(result() + "GET") }
                put<ReifiedPath> { result(result() + "PUT") }
                post<ReifiedPath> { result(result() + "POST") }
                patch<ReifiedPath> { result(result() + "PATCH") }
                delete<ReifiedPath> { result(result() + "DELETE") }
                head<ReifiedPath> { result(result() + "HEAD") }
                options<ReifiedPath> { result(result() + "OPTIONS") }
                afterMatched<ReifiedPath> { result(result() + " After-Matched") }
                after<ReifiedPath> {
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

    @Test
    fun `should support nested path scopes`() = JavalinTest.test(
        // given: a javalin app with routes defined using nested path scopes
        Javalin.create { config ->
            config.routes(Dsl) {
                path("api") {
                    get { result("api") }
                    get("users") { result("users") }

                    path("admin") {
                        get("users") { result("admin-users") }
                    }
                }
            }
        },
        defaultConfig
    ) { _, client ->
        assertThat(get("${client.origin}/api").asString().body).isEqualTo("api")
        assertThat(get("${client.origin}/api/users").asString().body).isEqualTo("users")
        assertThat(get("${client.origin}/api/admin/users").asString().body).isEqualTo("admin-users")
    }

    @Test
    fun `should apply pathless matched filters to nested path scope`() = JavalinTest.test(
        // given: a javalin app with pathless beforeMatched filters in nested path scopes
        Javalin.create { config ->
            config.routes(Dsl) {
                path("api") {
                    beforeMatched { result("api-") }

                    path("admin") {
                        beforeMatched { result(result() + "admin-") }
                        get("users") { result(result() + "users") }
                    }
                }
            }
        },
        defaultConfig
    ) { _, client ->
        assertThat(get("${client.origin}/api/admin/users").asString().body).isEqualTo("api-admin-users")
    }

    @Test
    fun `should apply explicit matched filters to exact nested path`() = JavalinTest.test(
        // given: a javalin app with explicit beforeMatched filter path in a nested path scope
        Javalin.create { config ->
            config.routes(Dsl) {
                path("api") {
                    beforeMatched("users") { result("before-") }
                    get("users") { result(result() + "users") }
                    get("other") { result("other") }
                }
            }
        },
        defaultConfig
    ) { _, client ->
        assertThat(get("${client.origin}/api/users").asString().body).isEqualTo("before-users")
        assertThat(get("${client.origin}/api/other").asString().body).isEqualTo("other")
    }

    @Path("/users/{id}")
    class NestedPathWithParameter(val id: String)

    @Test
    fun `should support class based routes in nested path scopes`() = JavalinTest.test(
        // given: a javalin app with a class based route in a nested path scope
        Javalin.create { config ->
            config.routes(Dsl) {
                path("api") {
                    get<NestedPathWithParameter> { result(it.id) }
                }
            }
        },
        defaultConfig
    ) { _, client ->
        assertThat(get("${client.origin}/api/users/123").asString().body).isEqualTo("123")
    }

    @Test
    fun `should keep top level pathless filter paths unchanged`() {
        // given: a routing configuration with top level pathless filters
        val config = DefaultDsl.createConfiguration()

        config.before { }
        config.beforeMatched { }
        config.after { }
        config.afterMatched { }

        // then: pathless filters keep the legacy empty path outside of path scopes
        assertThat(config.routes.map { it.path }).containsExactly("", "", "", "")
    }

    @Test
    fun `should prefix registered routes in nested path scopes`() = JavalinTest.test(
        // given: a javalin app with externally built routes registered in a nested path scope
        Javalin.create { config ->
            config.routes(Dsl) {
                path("api") {
                    register(
                        listOf(
                            DefaultDslRoute(Route.GET, "/users") {
                                result("registered-users")
                            }
                        )
                    )
                }
            }
        },
        defaultConfig
    ) { _, client ->
        assertThat(get("${client.origin}/api/users").asString().body).isEqualTo("registered-users")
    }

    @Test
    fun `pathless http methods should map to root`() = JavalinTest.test(
        // given: a javalin app with pathless http methods at the top level
        Javalin.create { config ->
            config.routes(Dsl) {
                get { result("root-get") }
                post { result("root-post") }
                put { result("root-put") }
                delete { result("root-delete") }
                patch { result("root-patch") }
                head { header("x-head", "root-head") }
                options { result("root-options") }
            }
        },
        defaultConfig
    ) { _, client ->
        // then: the methods are mapped to the root path
        assertThat(get(client.origin).asString().body).isEqualTo("root-get")
        assertThat(request("POST", client.origin).asString().body).isEqualTo("root-post")
        assertThat(request("PUT", client.origin).asString().body).isEqualTo("root-put")
        assertThat(request("DELETE", client.origin).asString().body).isEqualTo("root-delete")
        assertThat(request("PATCH", client.origin).asString().body).isEqualTo("root-patch")
        assertThat(request("HEAD", client.origin).asEmpty().headers.getFirst("x-head")).isEqualTo("root-head")
        assertThat(request("OPTIONS", client.origin).asString().body).isEqualTo("root-options")
    }

    @Test
    fun `should normalize various path formats in scopes`() = JavalinTest.test(
        // given: a javalin app with mixed path formats in nested scopes
        Javalin.create { config ->
            config.routes(Dsl) {
                path("/api/") {
                    get { result("api") }
                    path("v1") {
                        get("users") { result("v1-users") }
                    }
                }
            }
        },
        defaultConfig
    ) { _, client ->
        // then: all paths are normalized correctly
        assertThat(get("${client.origin}/api").asString().body).isEqualTo("api")
        assertThat(get("${client.origin}/api/v1/users").asString().body).isEqualTo("v1-users")
    }

    @Test
    fun `should support deeply nested path scopes`() = JavalinTest.test(
        // given: a javalin app with three levels of nested path scopes
        Javalin.create { config ->
            config.routes(Dsl) {
                path("a") {
                    path("b") {
                        path("c") {
                            get { result("deep") }
                        }
                    }
                }
            }
        },
        defaultConfig
    ) { _, client ->
        // then: the route is available at the combined path
        assertThat(get("${client.origin}/a/b/c").asString().body).isEqualTo("deep")
    }

    @Test
    fun `should handle empty path scope`() = JavalinTest.test(
        // given: a javalin app with an empty path scope
        Javalin.create { config ->
            config.routes(Dsl) {
                path("") {
                    get("users") { result("users") }
                }
            }
        },
        defaultConfig
    ) { _, client ->
        // then: the route is available at the inner path only
        assertThat(get("${client.origin}/users").asString().body).isEqualTo("users")
    }

}
