package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.community.routing.coroutines.servlet.DefaultContextCoroutinesServlet
import io.javalin.community.routing.coroutines.ReactiveRoute
import io.javalin.community.routing.coroutines.ReactiveRoutes
import io.javalin.community.routing.coroutines.reactiveRouting
import io.javalin.http.Context
import io.javalin.testtools.JavalinTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

class CoroutinesRoutingTest {

    @Test
    fun `shutdown on coroutines servlet closes underlying resources`() {
        val executor = Executors.newSingleThreadExecutor()

        DefaultContextCoroutinesServlet(executor) { it }
            .apply { prepareShutdown() }
            .apply { completeShutdown() }

        assertThat(executor.isShutdown).isTrue
    }

    @Test
    fun `should properly execute coroutine`() =
        JavalinTest.test(
            Javalin.create { config ->
                config.reactiveRouting(
                    DefaultContextCoroutinesServlet(Executors.newSingleThreadExecutor()) { it },
                    object : ReactiveRoutes<ReactiveRoute<Context, Unit>, Context, Unit>() {
                        override fun routes() = setOf(
                            reactiveRoute("/test", Route.GET) {
                                withContext(Dispatchers.IO) {
                                    result(Thread.currentThread().name)
                                }
                            }
                        )
                    }
                )
            }
        ) { _, client ->
            assertThat(client.get("/test").body?.string()).contains("DefaultDispatcher")
        }

    private class TestException : RuntimeException()

    @Test
    fun `javalin should be able to handle exceptions from coroutines`() =
        JavalinTest.test(
            Javalin.create { config ->
                config.reactiveRouting(
                    DefaultContextCoroutinesServlet(Executors.newSingleThreadExecutor()) { it },
                    object : ReactiveRoutes<ReactiveRoute<Context, Unit>, Context, Unit>() {
                        override fun routes() = setOf(
                            reactiveRoute("/test", Route.GET) {
                                withContext(Dispatchers.IO) {
                                    throw TestException()
                                }
                            }
                        )
                    }
                )
            }.exception(TestException::class.java) { _, ctx ->
                ctx.result("Handled")
            }
        ) { _, client ->
            assertThat(client.get("/test").body?.string()).isEqualTo("Handled")
        }

    @Test
    fun `should execute non-async handlers in regular thread pool`() =
        JavalinTest.test(
            Javalin.create { config ->
                config.reactiveRouting(
                    DefaultContextCoroutinesServlet(Executors.newSingleThreadExecutor()) { it },
                    object : ReactiveRoutes<ReactiveRoute<Context, Unit>, Context, Unit>() {
                        override fun routes() = setOf(
                            reactiveRoute("/test", Route.GET, async = false) {
                                result(Thread.currentThread().name)
                            }
                        )
                    }
                )
            }
        ) { _, client ->
            assertThat(client.get("/test").body?.string()).contains("JettyServerThreadPool")
        }

}