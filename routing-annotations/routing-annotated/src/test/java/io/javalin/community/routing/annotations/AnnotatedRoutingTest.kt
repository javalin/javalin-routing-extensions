package io.javalin.community.routing.annotations

import io.javalin.Javalin
import io.javalin.community.routing.Route
import io.javalin.community.routing.annotations.AnnotatedRouting.Annotated
import io.javalin.community.routing.routes
import io.javalin.event.JavalinLifecycleEvent.SERVER_STARTED
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.HttpStatus
import io.javalin.router.Endpoint
import io.javalin.testtools.HttpClient
import io.javalin.testtools.JavalinTest
import kong.unirest.Unirest
import kong.unirest.Unirest.request
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.Closeable
import java.util.*

class AnnotatedRoutingTest {

    @Nested
    inner class Paths {

        @Nested
        inner class Registration {

            private fun withinSharedScenario(test: (HttpClient) -> Unit) {
                JavalinTest.test(
                    Javalin.create { cfg ->
                        cfg.routes(Annotated) {
                            registerEndpoints(
                                @Endpoints("/test")
                                object {
                                    // formatter:off
                                    @Before fun beforeEach(ctx: Context) { ctx.header("before", "true") }
                                    @After fun afterEach(ctx: Context) { ctx.header("after", "true") }

                                    @Before("/specific") fun beforeSpecific(ctx: Context, endpoint: Endpoint?) {
                                        ctx.header("before", "specific")
                                        ctx.header("before-endpoint", endpoint?.path ?: "not-matched")
                                    }
                                    @BeforeMatched fun beforeEachMatched(ctx: Context, endpoint: Endpoint) {
                                        ctx.header("before-matched", "true")
                                        ctx.header("before-matched-endpoint", "${endpoint.method.name} ${endpoint.path}")
                                    }
                                    @AfterMatched fun afterEachMatched(ctx: Context, endpoint: Endpoint) {
                                        ctx.header("after-matched", "true")
                                        ctx.header("after-matched-endpoint", "${endpoint.method.name} ${endpoint.path}")
                                    }
                                    @After("/specific") fun afterSpecific(ctx: Context, endpoint: Endpoint?) {
                                        ctx.header("after", "specific")
                                        ctx.header("after-endpoint", endpoint?.path ?: "not-matched")
                                    }

                                    @Get("/get") fun testGet(ctx: Context) { ctx.header("get", "true") }
                                    @Post("/post") fun testPost(ctx: Context) { ctx.header("post", "true") }
                                    @Put("/put") fun testPut(ctx: Context) { ctx.header("put", "true") }
                                    @Delete("/delete") fun testDelete(ctx: Context) { ctx.header("delete", "true") }
                                    @Patch("/patch") fun testPatch(ctx: Context) { ctx.header("patch", "true") }
                                    @Head("/head") fun testHead(ctx: Context) { ctx.header("head", "true") }
                                    @Options("/options") fun testOptions(ctx: Context) { ctx.header("options", "true") }
                                    // formatter:on
                                }
                            )
                        }
                    }
                ) { _, client -> test(client) }
            }

            @Test
            fun `should register all annotated http endpoints with their interceptors`() {
                withinSharedScenario { client ->
                    Route.entries
                        .filter { it.isHttpMethod }
                        .forEach {
                            val response = request(it.name, "${client.origin}/test/${it.name.lowercase()}").asEmpty()
                            assertThat(response.headers.getFirst(it.name.lowercase())).isEqualTo("true")

                            assertThat(response.headers.getFirst("before")).isEqualTo("true")
                            assertThat(response.headers.getFirst("before-endpoint")).isEmpty()
                            assertThat(response.headers.getFirst("before-matched")).isEqualTo("true")
                            assertThat(response.headers.getFirst("before-matched-endpoint")).isEqualTo("${it.name} /test/${it.name.lowercase()}")

                            assertThat(response.headers.getFirst("after")).isEqualTo("true")
                            assertThat(response.headers.getFirst("after-endpoint")).isEmpty()
                            assertThat(response.headers.getFirst("after-matched")).isEqualTo("true")
                            assertThat(response.headers.getFirst("after-matched-endpoint")).isEqualTo("${it.name} /test/${it.name.lowercase()}")
                        }
                }
            }

            @Test
            fun `should register all possible before handlers`() {
                withinSharedScenario { client ->
                    val beforeAtRootLevel = request("GET", "${client.origin}/test").asEmpty()
                    assertThat(beforeAtRootLevel.headers.getFirst("before")).isEqualTo("true")

                    val beforeAtRootLevelWithTrailingSlash = request("GET", "${client.origin}/test/").asEmpty()
                    assertThat(beforeAtRootLevelWithTrailingSlash.headers.getFirst("before")).isEqualTo("true")

                    val beforeAtRootLevelWithWildcard = request("GET", "${client.origin}/test/a/b/c").asEmpty()
                    assertThat(beforeAtRootLevelWithWildcard.headers.getFirst("before")).isEqualTo("true")
                }
            }

            @Test
            fun `should register before handler only at the explicitly specified path`() {
                withinSharedScenario { client ->
                    val beforeSpecific = request("GET", "${client.origin}/test/specific").asEmpty()
                    assertThat(beforeSpecific.headers.getFirst("before")).isEqualTo("specific")
                    assertThat(beforeSpecific.headers.getFirst("before-endpoint")).isEqualTo("not-matched")

                    val beforeTooSpecific = request("GET", "${client.origin}/test/specific/too-specific").asEmpty()
                    assertThat(beforeTooSpecific.headers.getFirst("before")).isNotEqualTo("specific")
                    assertThat(beforeTooSpecific.headers.getFirst("before-endpoint")).isEmpty()
                }
            }

            @Test
            fun `should register after handler only at the explicitly specified path`() {
                withinSharedScenario { client ->
                    val afterSpecific = request("GET", "${client.origin}/test/specific").asEmpty()
                    assertThat(afterSpecific.headers.getFirst("after")).isEqualTo("specific")
                    assertThat(afterSpecific.headers.getFirst("after-endpoint")).isEqualTo("not-matched")

                    val afterTooSpecific = request("GET", "${client.origin}/test/specific/too-specific").asEmpty()
                    assertThat(afterTooSpecific.headers.getFirst("after")).isNotEqualTo("specific")
                    assertThat(afterTooSpecific.headers.getFirst("after-endpoint")).isEmpty()
                }
            }

            @Test
            fun `should register all possible after handlers`() {
                withinSharedScenario { client ->
                    val afterAtRootLevel = request("GET", "${client.origin}/test").asEmpty()
                    assertThat(afterAtRootLevel.headers.getFirst("after")).isEqualTo("true")

                    val afterAtRootLevelWithTrailingSlash = request("GET", "${client.origin}/test/").asEmpty()
                    assertThat(afterAtRootLevelWithTrailingSlash.headers.getFirst("after")).isEqualTo("true")

                    val afterAtRootLevelWithWildcard = request("GET", "${client.origin}/test/a/b/c").asEmpty()
                    assertThat(afterAtRootLevelWithWildcard.headers.getFirst("after")).isEqualTo("true")
                }
            }

            @Test
            fun `should not register before and after handlers below the specified path`() {
                withinSharedScenario { client ->
                    val beforeAtRootLevel = request("GET", "${client.origin}/").asEmpty()
                    assertThat(beforeAtRootLevel.headers.getFirst("before")).isEmpty()

                    val afterAtRootLevel = request("GET", "${client.origin}/").asEmpty()
                    assertThat(afterAtRootLevel.headers.getFirst("after")).isEmpty()
                }
            }
        }

        @Test
        fun `should sanitize repeated path separators`() {
            val app = Javalin.create { cfg ->
                cfg.routes(Annotated) {
                    registerEndpoints(
                        @Endpoints("/test/")
                        object {
                            @Get("/with")
                            fun get(ctx: Context) {
                            }
                        },
                        @Endpoints("test")
                        object {
                            @Get("without")
                            fun get(ctx: Context) {
                            }
                        }
                    )
                }
            }

            val matcher = app.unsafe.internalRouter

            assertThat(matcher.findHttpHandlerEntries(HandlerType.GET, "/test/with"))
                .hasSize(1)
                .allMatch { it.endpoint.path == "/test/with" }

            assertThat(matcher.findHttpHandlerEntries(HandlerType.GET, "/test/without"))
                .hasSize(1)
                .allMatch { it.endpoint.path == "/test/without" }
        }

        @Test
        fun `should skip methods in endpoint class that are not annotated`() {
            assertDoesNotThrow {
                Javalin.create { cfg ->
                    cfg.routes(Annotated) {
                        registerEndpoints(
                            @Endpoints
                            object {
                                fun regularMethod() {}
                            }
                        )
                    }
                }
            }
        }
    }

    @Nested
    inner class Injections {

        @Test
        fun `should inject all supported properties from context`() =
            JavalinTest.test(
                Javalin.create { cfg ->
                    cfg.validation.register(UUID::class.java) { value ->
                        runCatching { UUID.fromString(value) }.getOrNull()
                    }

                    cfg.routes(Annotated) {
                        registerEndpoints(
                            @Endpoints
                            object {
                                @Post("/test/{param}/{param2}/{param3}")
                                fun test(
                                    ctx: Context,
                                    @Param param: Int,
                                    @Param("param2") param2: UUID,
                                    @Param("param3") optionalParam: Optional<UUID>,
                                    @Header header: Int,
                                    @Header optionalHeader: Optional<String>,
                                    @Query query: Int,
                                    @Cookie cookie: Int,
                                    @Body body: Int,
                                ) {
                                    ctx.header("param", param.toString())
                                    ctx.header("param2", param2.toString())
                                    ctx.header("optionalParam", optionalParam.orElse(null)?.toString() ?: "null")
                                    ctx.header("header", header.toString())
                                    ctx.header("optionalHeader", optionalHeader.orElse("default"))
                                    ctx.header("query", query.toString())
                                    ctx.header("cookie", cookie.toString())
                                    ctx.header("body", body.toString())
                                }
                            }
                        )
                    }
                }
            ) { _, client ->
                val response = Unirest.post("${client.origin}/test/1/00000000-0000-0000-0000-000000000000/bad-value")
                    .header("header", "2")
                    .queryString("query", "3")
                    .cookie("cookie", "4")
                    .body("5")
                    .asEmpty()
                assertThat(response.status).isEqualTo(200)

                val responseHeaders = response.headers
                assertThat(responseHeaders.getFirst("param")).isEqualTo("1")
                assertThat(responseHeaders.getFirst("param2")).isEqualTo("00000000-0000-0000-0000-000000000000")
                assertThat(responseHeaders.getFirst("optionalParam")).isEqualTo("null")
                assertThat(responseHeaders.getFirst("header")).isEqualTo("2")
                assertThat(responseHeaders.getFirst("optionalHeader")).isEqualTo("default")
                assertThat(responseHeaders.getFirst("query")).isEqualTo("3")
                assertThat(responseHeaders.getFirst("cookie")).isEqualTo("4")
                assertThat(responseHeaders.getFirst("body")).isEqualTo("5")
            }

        @Test
        fun `should respond with bad request if property cannot be mapped into parameter`() =
            JavalinTest.test(
                Javalin.create { cfg ->
                    cfg.routes(Annotated) {
                        registerEndpoints(
                            @Endpoints
                            object {
                                @Get("/test/{param}")
                                fun test(@Param param: Int) = Unit
                            }
                        )
                    }
                }
            ) { _, client ->
                val response = Unirest.get("${client.origin}/test/abc").asString()
                assertThat(response.status).isEqualTo(400)
            }


        @Test
        fun `should throw exception if route has unsupported parameter in signature`() {
            assertThatThrownBy {
                Javalin.create().unsafe.routes(Annotated) {
                    registerEndpoints(
                        @Endpoints
                        object {
                            @Get("/test") fun test(ctx: Context, unsupported: String) {}
                        }
                    )
                }
            }
            .isExactlyInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unsupported parameter type")
        }

    }


    @Nested
    inner class Versioning {

        @Test
        fun `should throw if two routes with the same versions are found`() {
            assertThatThrownBy {
                Javalin.create {
                    it.routes(Annotated) {
                        registerEndpoints(
                            @Endpoints("/api/users")
                            object {
                                @Version("1")
                                @Get
                                fun findAll(ctx: Context) {
                                    ctx.result("Panda")
                                }
                            },
                            @Endpoints("/api/users")
                            object {
                                @Version("1")
                                @Get
                                fun test(ctx: Context) {
                                    ctx.result("Red Panda")
                                }
                            }
                        )
                    }
                }
            }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Duplicated version found for the same route: GET /api/users (versions: [1, 1])")
        }

        @Test
        fun `should properly serve versioned routes`() =
            JavalinTest.test(
                Javalin.create { cfg ->
                    cfg.routes(Annotated) {
                        registerEndpoints(
                            @Endpoints("/api/users")
                            object {
                                @Version("1")
                                @Get
                                fun findAll(ctx: Context) {
                                    ctx.result("Panda")
                                }
                            },
                            @Endpoints("/api/users")
                            object {
                                @Version("2")
                                @Get
                                fun test(ctx: Context) {
                                    ctx.result("Red Panda")
                                }
                            }
                        )
                    }
                }
            ) { _, client ->
                val v1 = Unirest.get("${client.origin}/api/users").header("X-API-Version", "1").asString().body
                val v2 = Unirest.get("${client.origin}/api/users").header("X-API-Version", "2").asString().body
                val v3 = Unirest.get("${client.origin}/api/users").header("X-API-Version", "3").asString().body

                assertThat(v1).isEqualTo("Panda")
                assertThat(v2).isEqualTo("Red Panda")
                assertThat(v3).isEqualTo("This endpoint does not support the requested API version (3).")
            }

    }

    @Nested
    inner class Async {

        @Test
        fun `should run async method in async context`() {
            JavalinTest.test(
                Javalin.create { cfg ->
                    cfg.routes(Annotated) {
                        registerEndpoints(
                            @Endpoints
                            object {
                                // formatter:off
                                @Before("/test") fun before(ctx: Context) { ctx.header("sync", Thread.currentThread().name) }
                                @Get("/test", async = true) fun test(ctx: Context) { ctx.header("async", Thread.currentThread().name) }
                                // formatter:on
                            }
                        )
                    }
                }
            ) { _, client ->
                val response = Unirest.get("${client.origin}/test").asEmpty()

                val sync = response.headers.getFirst("sync")
                assertThat(sync).isNotNull

                val async = response.headers.getFirst("async")
                assertThat(async).isNotNull

                assertThat(sync).isNotEqualTo(async)
            }
        }

    }

    @Nested
    inner class Exceptions {

        @Test
        fun `should properly handle exceptions`() =
            JavalinTest.test(
                Javalin.create {
                    it.routes(Annotated) { cfg ->
                        cfg.registerEndpoints(object {
                            @Get("/throwing")
                            fun throwing(ctx: Context): Nothing = throw IllegalStateException("This is a test")

                            @ExceptionHandler(IllegalStateException::class)
                            fun handleException(ctx: Context, e: IllegalStateException) {
                                ctx.result(e::class.java.name)
                            }
                        })
                    }
                }
            ) { _, client ->
                assertThat(Unirest.get("${client.origin}/throwing").asString().body).isEqualTo("java.lang.IllegalStateException")
            }

    }

    @Nested
    inner class LifecycleEvents {

        @Test
        fun `should handle lifecycle events`() {
            val log = mutableListOf<String>()

            JavalinTest.test(
                Javalin.create {
                    it.routes(Annotated) { cfg ->
                        cfg.registerEndpoints(object {
                            @LifecycleEventHandler(SERVER_STARTED)
                            fun onStart() {
                                log.add("Started")
                            }
                        })
                    }
                }
            ) { _, _ ->
                assertThat(log).containsExactly("Started")
            }
        }

    }

    @Nested
    inner class Results {

        @Test
        fun `should use status code from annotation`() =
            JavalinTest.test(
                Javalin.create { cfg ->
                    cfg.routes(Annotated) {
                        registerEndpoints(
                            object {
                                @Get("/test")
                                @Status(success = HttpStatus.IM_A_TEAPOT)
                                fun test(ctx: Context): String = "abc"
                            }
                        )
                    }
                }
            ) { _, client ->
                assertThat(Unirest.get("${client.origin}/test").asString().status).isEqualTo(HttpStatus.IM_A_TEAPOT.code)
            }

        @Test
        fun `should throw for unsupported return types`() {
            assertThatThrownBy {
                Javalin.create {
                    it.routes(Annotated) { cfg ->
                        cfg.registerEndpoints(
                            object {
                                @Get("/unsupported")
                                fun unsupported(ctx: Context): Int = 1
                            }
                        )
                    }
                }
            }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Unsupported return type: int")
        }

        private open inner class Animal
        private open inner class Panda : Animal()
        private open inner class RedPanda : Panda()

        @Test
        fun `should properly handle inheritance`() =
            JavalinTest.test(
                Javalin.create { cfg ->
                    cfg.routes(Annotated) {
                        registerResultHandler<Animal> { ctx, _ -> ctx.result("Animal") }
                        registerResultHandler<RedPanda> { ctx, _ -> ctx.result("RedPanda") }
                        registerResultHandler<Panda> { ctx, _ -> ctx.result("Panda") }

                        registerEndpoints(
                            object {
                                @Get("/base")
                                fun base(ctx: Context): Animal = Animal()

                                @Get("/closest")
                                fun closest(ctx: Context): RedPanda = RedPanda()
                            }
                        )
                    }
                }
            ) { _, client ->
                assertThat(Unirest.get("${client.origin}/base").asString().body).isEqualTo("Animal")
                assertThat(Unirest.get("${client.origin}/closest").asString().body).isEqualTo("RedPanda")
            }

        private open inner class GiantPanda : Panda(), Closeable {
            override fun close() {}
        }

        @Test
        fun `should throw if result handler matched multiple classes`() {
            assertThatThrownBy {
                Javalin.create { cfg ->
                    cfg.routes(Annotated) {
                        registerResultHandler<Panda> { ctx, _ -> ctx.result("Panda") }
                        registerResultHandler<Closeable> { ctx, _ -> ctx.result("Closeable") }
                        registerEndpoints(object {
                            @Get("/test")
                            fun test(ctx: Context): GiantPanda = GiantPanda()
                        })
                    }
                }
            }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Unable to determine handler for type class")
        }

    }

}