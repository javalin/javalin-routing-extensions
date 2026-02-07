package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.Route.PUT
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.util.Util.firstOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JavalinRoutesTest {
    
    @Test
    fun `should properly register handler by given enum`() {
        // given: a list of routes
        val routes = Route.entries
            .map { it to Handler { ctx -> ctx.result(it.name) } }

        // when: routes are registered
        val app = Javalin.create()
        app.unsafe.internalRouter.apply { routes.forEach { route -> registerRoute(route.first, "/", route.second) } }

        // then: all routes are registered by as proper HandlerType
        routes.forEach { (method, handler) ->
            val endpoint = app
                .unsafe
                .internalRouter
                .findHttpHandlerEntries(HandlerType.values().first { it.name == method.name }, "/")
                .firstOrNull()
                ?.endpoint

            assertThat(endpoint?.method?.name).isEqualTo(method.name)
            assertThat(endpoint?.path).isEqualTo("/")
        }
    }

    @Test
    fun `should register routes`() {
        val application = JavalinRoutingExtensions(Javalin.create())
            .addRoute(GET, "/") { it.result("Hello World!") }
            .addRoute(PUT, "/") { it.result("Hello World!") }
            .register()

        listOf("GET", "PUT").forEach { methodName ->
            assertThat(
                application
                    .unsafe
                    .internalRouter
                    .findHttpHandlerEntries(HandlerType.values().first { it.name == methodName }, "/")
                    .firstOrNull()
            ).isNotNull
        }
    }

    @Test
    fun `should enable sam receiver for mount`() {
        Javalin.create { cfg ->
            cfg.routes {
                get("/") { it.result("Hello World!") }
            }
        }
    }

}