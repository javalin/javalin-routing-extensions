package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.community.routing.coroutines.DefaultContextCoroutinesServlet
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
    fun `should properly execute coroutine`() =
        JavalinTest.test(
            Javalin.create { config ->
                config.reactiveRouting(
                    DefaultContextCoroutinesServlet(Executors.newSingleThreadExecutor()) { it },
                    object : ReactiveRoutes<ReactiveRoute<Context, Unit>, Context, Unit>() {
                        override fun routes() = setOf(
                            reactiveRoute("/test", Route.GET) {
                                withContext(Dispatchers.IO) {
                                    result("OK")
                                }
                            }
                        )
                    }
                )
            }
        ) { _, client ->
            assertThat(client.get("/test").body?.string()).isEqualTo("OK")
        }

}