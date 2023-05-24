package io.javalin.community.routing.annotations

import io.javalin.Javalin
import io.javalin.community.routing.Route
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.HttpStatus
import io.javalin.testtools.JavalinTest
import kong.unirest.Unirest
import kong.unirest.Unirest.request
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class AnnotatedRoutingTest {

    @Test
    fun `should sanitize repeated path separators`() {
        val app = Javalin.create {
            it.registerAnnotatedEndpoints(
                @Endpoints("/test/")
                object {
                    @Get("/with")
                    fun get(ctx: Context) {}
                },
                @Endpoints("test")
                object {
                    @Get("without")
                    fun get(ctx: Context) {}
                }
            )
        }

        val matcher = app.javalinServlet().matcher

        assertThat(matcher.findEntries(HandlerType.GET, "/test/with"))
            .hasSize(1)
            .allMatch { it.path == "/test/with" }

        assertThat(matcher.findEntries(HandlerType.GET, "/test/without"))
            .hasSize(1)
            .allMatch { it.path == "/test/without" }
    }

    @Test
    fun `should throw exception if route has unsupported parameter in signature`() {
        assertThatThrownBy {
            AnnotatedRoutingPlugin().registerEndpoints(
                @Endpoints
                object {
                    @Get("/test")
                    fun test(ctx: Context, unsupported: String) {}
                }
            )
        }
        .isExactlyInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("Unsupported parameter type")
    }

    @Test
    fun `should properly register all annotated endpoints`() =
        JavalinTest.test(
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    @Endpoints("/test")
                    object {
                        @Before
                        fun beforeEach(ctx: Context) { ctx.header("before", "true") }
                        @After
                        fun afterEach(ctx: Context) { ctx.header("after", "true") }
                        @Get("/get")
                        fun testGet(ctx: Context) { ctx.header("get", "true") }
                        @Post("/post")
                        fun testPost(ctx: Context) { ctx.header("post", "true") }
                        @Put("/put")
                        fun testPut(ctx: Context) { ctx.header("put", "true") }
                        @Delete("/delete")
                        fun testDelete(ctx: Context) { ctx.header("delete", "true") }
                        @Patch("/patch")
                        fun testPatch(ctx: Context) { ctx.header("patch", "true") }
                        @Head("/head")
                        fun testHead(ctx: Context) { ctx.header("head", "true") }
                        @Options("/options")
                        fun testOptions(ctx: Context) { ctx.header("options", "true") }
                    }
                )
            }
        ) { _, client ->
            Route.values()
                .filter { it.isHttpMethod }
                .forEach {
                    val response = request(it.name, "${client.origin}/test/${it.name.lowercase()}").asEmpty()
                    assertThat(response.headers.getFirst(it.name.lowercase())).isEqualTo("true")
                    assertThat(response.headers.getFirst("before")).isEqualTo("true")
                    assertThat(response.headers.getFirst("after")).isEqualTo("true")
                }
        }

    @Test
    fun `should run async method in async context`() {
        JavalinTest.test(
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    @Endpoints
                    object {
                        @Before("/test")
                        fun before(ctx: Context) { ctx.header("sync", Thread.currentThread().name) }
                        @Get("/test", async = true)
                        fun test(ctx: Context) { ctx.header("async", Thread.currentThread().name) }
                    }
                )
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

    @Test
    fun `should inject all supported properties from context`() =
        JavalinTest.test(
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    @Endpoints
                    object {
                        @Post("/test/{param}")
                        fun test(
                            ctx: Context,
                            @Param param: Int,
                            @Header header: Int,
                            @Query query: Int,
                            @Cookie cookie: Int,
                            @Body body: Int,
                        ) {
                            ctx.header("param", param.toString())
                            ctx.header("header", header.toString())
                            ctx.header("query", query.toString())
                            ctx.header("cookie", cookie.toString())
                            ctx.header("body", body.toString())
                        }
                    }
                )
            }
        ) { _, client ->
            val responseHeaders = Unirest.post("${client.origin}/test/1")
                .header("header", "2")
                .queryString("query", "3")
                .cookie("cookie", "4")
                .body("5")
                .asEmpty()
                .headers

            assertThat(responseHeaders.getFirst("param")).isEqualTo("1")
            assertThat(responseHeaders.getFirst("header")).isEqualTo("2")
            assertThat(responseHeaders.getFirst("query")).isEqualTo("3")
            assertThat(responseHeaders.getFirst("cookie")).isEqualTo("4")
            assertThat(responseHeaders.getFirst("body")).isEqualTo("5")
        }

    @Test
    fun `should respond with bad request if property cannot be mapped into parameter`() =
        JavalinTest.test(
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    @Endpoints
                    object {
                        @Get("/test/{param}")
                        fun test(@Param param: Int) = Unit
                    }
                )
            }
        ) { _, client ->
            val response = Unirest.get("${client.origin}/test/abc").asString()
            assertThat(response.status).isEqualTo(400)
        }

    @Test
    fun `should skip methods in endpoint class that are not annotated`() {
        assertDoesNotThrow {
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    @Endpoints
                    object {
                        fun regularMethod() {}
                    }
                )
            }
        }
    }

    @Test
    fun `should throw if two routes with the same versions are found`() {
        assertThatThrownBy {
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    @Endpoints("/api/users")
                    object {
                        @Version("1")
                        @Get
                        fun findAll(ctx: Context) { ctx.result("Panda") }
                    },
                    @Endpoints("/api/users")
                    object {
                        @Version("1")
                        @Get
                        fun test(ctx: Context) { ctx.result("Red Panda") }
                    }
                )
            }
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("Duplicated version found for the same route: GET /api/users/ (versions: [1, 1])")
    }

    @Test
    fun `should properly serve versioned routes`() =
        JavalinTest.test(
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    @Endpoints("/api/users")
                    object {
                        @Version("1")
                        @Get
                        fun findAll(ctx: Context) { ctx.result("Panda") }
                    },
                    @Endpoints("/api/users")
                    object {
                        @Version("2")
                        @Get
                        fun test(ctx: Context) { ctx.result("Red Panda") }
                    }
                )
            }
        ) { _, client ->
            val v1 = Unirest.get("${client.origin}/api/users").header("X-API-Version", "1").asString().body
            val v2 = Unirest.get("${client.origin}/api/users").header("X-API-Version", "2").asString().body
            val v3 = Unirest.get("${client.origin}/api/users").header("X-API-Version", "3").asString().body

            assertThat(v1).isEqualTo("Panda")
            assertThat(v2).isEqualTo("Red Panda")
            assertThat(v3).isEqualTo("This endpoint does not support the requested API version (3).")
        }

    @Test
    fun `should properly handle exceptions`() =
        JavalinTest.test(
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    object {
                        @Get("/throwing")
                        fun throwing(ctx: Context): Nothing = throw IllegalStateException("This is a test")
                        @ExceptionHandler(IllegalStateException::class)
                        fun handleException(ctx: Context, e: IllegalStateException) { ctx.result(e::class.java.name) }
                    }
                )
            }
        ) { _, client ->
            assertThat(Unirest.get("${client.origin}/throwing").asString().body).isEqualTo("java.lang.IllegalStateException")
        }

    @Test
    fun `should throw for unsupported return types`() {
        assertThatThrownBy {
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    object {
                        @Get("/unsupported")
                        fun unsupported(ctx: Context): Int = 1
                    }
                )
            }
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("Unsupported return type: int")
    }

    private open class Animal
    private open class Panda : Animal()
    private open class RedPanda : Panda()

    @Test
    fun `should properly handle inheritance`() =
        JavalinTest.test(
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    configuration = AnnotatedRoutingPluginConfiguration()
                        .registerResultHandler<Animal> { ctx, _ -> ctx.result("Animal") }
                        .registerResultHandler<RedPanda> { ctx, _ -> ctx.result("RedPanda") }
                        .registerResultHandler<Panda> { ctx, _ -> ctx.result("Panda") },
                    object {
                        @Get("/base")
                        fun base(ctx: Context): Animal = Animal()
                        @Get("/closest")
                        fun closest(ctx: Context): RedPanda = RedPanda()
                    }
                )
            }
        ) { _, client ->
            assertThat(Unirest.get("${client.origin}/base").asString().body).isEqualTo("Animal")
            assertThat(Unirest.get("${client.origin}/closest").asString().body).isEqualTo("RedPanda")
        }

    interface Heavy
    private open class GiantPanda : Panda(), Heavy

    @Test
    fun `should throw if result handler matched multiple classes`() {
        assertThatThrownBy {
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    configuration = AnnotatedRoutingPluginConfiguration()
                        .registerResultHandler<Panda> { ctx, _ -> ctx.result("Panda") }
                        .registerResultHandler<Heavy> { ctx, _ -> ctx.result("Heavy") },
                    object {
                        @Get("/test")
                        fun test(ctx: Context): GiantPanda = GiantPanda()
                    }
                )
            }
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Unable to determine handler for type class")
    }

    @Test
    fun `should use status code from result handler`() =
        JavalinTest.test(
            Javalin.create {
                it.registerAnnotatedEndpoints(
                    object {
                        @Get("/test")
                        @Status(success = HttpStatus.IM_A_TEAPOT)
                        fun test(ctx: Context): String = "abc"
                    }
                )
            }
        ) { _, client ->
            assertThat(Unirest.get("${client.origin}/test").asString().status).isEqualTo(HttpStatus.IM_A_TEAPOT.code)
        }

}