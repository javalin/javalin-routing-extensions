package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.config.RouterConfig
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.router.Endpoint
import io.javalin.router.InternalRouter
import io.javalin.router.JavalinDefaultRouting
import io.javalin.router.RoutingApiInitializer
import io.javalin.router.RoutingSetupScope
import io.javalin.security.RouteRole
import java.util.function.Consumer

class JavalinRoutingExtensions(private val javalin: Javalin) {

    private val routes = mutableListOf<HandlerEntry>()

    fun addRouteHandler(vararg handlerEntry: HandlerEntry): JavalinRoutingExtensions = also {
        routes.addAll(handlerEntry)
    }

    @JvmOverloads
    fun addRoute(route: Route, path: String, vararg roles: RouteRole = emptyArray(), handler: Handler): JavalinRoutingExtensions = also {
        routes.add(HandlerEntry(route, path, handler, roles.toList()))
    }

    fun register(): Javalin {
        routes
            .sortRoutes()
            .forEach { javalin.unsafeConfig().pvt.internalRouter.registerRoute(it) }

        return javalin
    }

}

data class HandlerEntry @JvmOverloads constructor(
    val route: Route,
    override val path: String,
    val handler: Handler,
    val roles: List<RouteRole> = emptyList(),
) : Routed

fun InternalRouter.registerRoute(handlerEntry: HandlerEntry) =
    registerRoute(handlerEntry.route, handlerEntry.path, handlerEntry.handler, *handlerEntry.roles.toTypedArray())

fun InternalRouter.registerRoute(route: Route, path: String, handler: Handler, vararg roles: RouteRole) {
    when (route) {
        Route.HEAD -> addHttpEndpoint(Endpoint(method = HandlerType.HEAD, path = path, handler = handler, roles = roles))
        Route.PATCH -> addHttpEndpoint(Endpoint(method = HandlerType.PATCH, path = path, handler = handler, roles = roles))
        Route.OPTIONS -> addHttpEndpoint(Endpoint(method = HandlerType.OPTIONS, path = path, handler = handler, roles = roles))
        Route.GET -> addHttpEndpoint(Endpoint(method = HandlerType.GET, path = path, handler = handler, roles = roles))
        Route.PUT -> addHttpEndpoint(Endpoint(method = HandlerType.PUT, path = path, handler = handler, roles = roles))
        Route.POST -> addHttpEndpoint(Endpoint(method = HandlerType.POST, path = path, handler = handler, roles = roles))
        Route.DELETE -> addHttpEndpoint(Endpoint(method = HandlerType.DELETE, path = path, handler = handler, roles = roles))
        Route.BEFORE -> addHttpEndpoint(Endpoint(method = HandlerType.BEFORE, path = path, handler = handler))
        Route.BEFORE_MATCHED -> addHttpEndpoint(Endpoint(method = HandlerType.BEFORE_MATCHED, path = path, handler = handler))
        Route.AFTER -> addHttpEndpoint(Endpoint(method = HandlerType.AFTER, path = path, handler = handler))
        Route.AFTER_MATCHED -> addHttpEndpoint(Endpoint(method = HandlerType.AFTER_MATCHED, path = path, handler = handler))
    }
}

fun RouterConfig.mount(setup: RoutingSetupScope<JavalinDefaultRouting>): RouterConfig = also {
    mount(Consumer {
        setup.invokeAsSamWithReceiver(it)
    })
}

fun <SETUP> RouterConfig.mount(initializer: RoutingApiInitializer<SETUP>, setup: RoutingSetupScope<SETUP> = RoutingSetupScope {}): RouterConfig = also {
    mount(initializer, Consumer {
        setup.invokeAsSamWithReceiver(it)
    })
}

fun <SETUP> RoutingSetupScope<SETUP>.invokeAsSamWithReceiver(receiver: SETUP) {
    with(this) { receiver.setup() }
}