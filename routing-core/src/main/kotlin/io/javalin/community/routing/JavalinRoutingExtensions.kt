package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.config.RouterConfig
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.router.*
import io.javalin.security.Roles
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
    registerRoute(
        route = handlerEntry.route,
        path = handlerEntry.path,
        handler = handlerEntry.handler,
        metadata = arrayOf(Roles(handlerEntry.roles.toSet())),
    )

@Suppress("DEPRECATION")
fun InternalRouter.registerRoute(route: Route, path: String, handler: Handler, vararg metadata: EndpointMetadata) {
    when (route) {
        Route.HEAD -> addHttpEndpoint(Endpoint.create(HandlerType.HEAD, path).metadata(*metadata).handler(handler))
        Route.PATCH -> addHttpEndpoint(Endpoint.create(HandlerType.PATCH, path).metadata(*metadata).handler(handler))
        Route.OPTIONS -> addHttpEndpoint(Endpoint.create(HandlerType.OPTIONS, path).metadata(*metadata).handler(handler))
        Route.GET -> addHttpEndpoint(Endpoint.create(HandlerType.GET, path).metadata(*metadata).handler(handler))
        Route.PUT -> addHttpEndpoint(Endpoint.create(HandlerType.PUT, path).metadata(*metadata).handler(handler))
        Route.POST -> addHttpEndpoint(Endpoint.create(HandlerType.POST, path).metadata(*metadata).handler(handler))
        Route.DELETE -> addHttpEndpoint(Endpoint.create(HandlerType.DELETE, path).metadata(*metadata).handler(handler))
        Route.BEFORE -> addHttpEndpoint(Endpoint.create(HandlerType.BEFORE, path).metadata(*metadata).handler(handler))
        Route.BEFORE_MATCHED -> addHttpEndpoint(Endpoint.create(HandlerType.BEFORE_MATCHED, path).metadata(*metadata).handler(handler))
        Route.AFTER -> addHttpEndpoint(Endpoint.create(HandlerType.AFTER, path).metadata(*metadata).handler(handler))
        Route.AFTER_MATCHED -> addHttpEndpoint(Endpoint.create(HandlerType.AFTER_MATCHED, path).metadata(*metadata).handler(handler))
    }
}

fun Endpoint.Companion.EndpointBuilder.metadata(vararg metadata: EndpointMetadata): Endpoint.Companion.EndpointBuilder =
    metadata.fold(this) { acc, value -> acc.addMetadata(value) }

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