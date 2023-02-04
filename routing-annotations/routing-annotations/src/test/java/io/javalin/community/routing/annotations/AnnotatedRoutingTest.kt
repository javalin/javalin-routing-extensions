package io.javalin.community.routing.annotations

import io.javalin.Javalin
import io.javalin.community.routing.Route
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.testtools.JavalinTest
import kong.unirest.Unirest
import kong.unirest.Unirest.request
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AnnotatedRoutingTest {

    @Test
    fun `should throw exception when endpoint class is not annotated`() {
        assertThatThrownBy {
            AnnotationsRoutingPlugin().registerEndpoints(
                object {
                    @Get("/test")
                    fun test(ctx: Context) = ctx.result("test")
                }
            )
        }
        .isExactlyInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("Endpoint class must be annotated with @Endpoints")
    }

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
            AnnotationsRoutingPlugin().registerEndpoints(
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
                        fun beforeEach(ctx: Context) = ctx.header("before", "true")
                        @After
                        fun afterEach(ctx: Context) = ctx.header("after", "true")
                        @Get("/get")
                        fun testGet(ctx: Context) = ctx.header("get", "true")
                        @Post("/post")
                        fun testPost(ctx: Context) = ctx.header("post", "true")
                        @Put("/put")
                        fun testPut(ctx: Context) = ctx.header("put", "true")
                        @Delete("/delete")
                        fun testDelete(ctx: Context) = ctx.header("delete", "true")
                        @Patch("/patch")
                        fun testPatch(ctx: Context) = ctx.header("patch", "true")
                        @Head("/head")
                        fun testHead(ctx: Context) = ctx.header("head", "true")
                        @Options("/options")
                        fun testOptions(ctx: Context) = ctx.header("options", "true")
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
                        fun before(ctx: Context) = ctx.header("sync", Thread.currentThread().name)
                        @Get("/test", async = true)
                        fun test(ctx: Context) = ctx.header("async", Thread.currentThread().name)
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

}