package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.community.routing.coroutines.Coroutines
import io.javalin.community.routing.coroutines.servlet.DefaultContextCoroutinesServlet
import io.javalin.testtools.JavalinTest
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CoroutinesRoutingTest {

    @Test
    fun `shutdown on coroutines servlet closes underlying resources`() {
        val executor = Executors.newSingleThreadExecutor()

        DefaultContextCoroutinesServlet(executor) { it }
            .apply { prepareShutdown() }
            .apply { completeShutdown() }

        assertThat(executor.isShutdown).isTrue
    }

    private val coroutines = Coroutines(DefaultContextCoroutinesServlet(Executors.newSingleThreadExecutor()) { it })

    @Test
    fun `should properly execute coroutine`() =
        JavalinTest.test(
            Javalin.create { config ->
                config.router.mount(coroutines) {
                    it.route(Route.GET, "/test") {
                        withContext(Dispatchers.IO) {
                            result(Thread.currentThread().name)
                        }
                    }
                }
            }
        ) { _, client ->
            assertThat(client.get("/test").body?.string()).contains("DefaultDispatcher")
        }

    private class TestException : RuntimeException()

    @Test
    fun `javalin should be able to handle exceptions from coroutines`() =
        JavalinTest.test(
            Javalin.create { config ->
                config.router.mount(coroutines) {
                    it.route(Route.GET, "/test") {
                        withContext(Dispatchers.IO) {
                            throw TestException()
                        }
                    }
                }
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
                config.router.mount(coroutines) {
                    it.route(Route.GET, "/test", async = false) {
                        result(Thread.currentThread().name)
                    }
                }
            }
        ) { _, client ->
            assertThat(client.get("/test").body?.string()).contains("JettyServerThreadPool")
        }

}