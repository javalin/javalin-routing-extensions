package io.javalin.community.routing.dsl

import io.javalin.community.routing.Route
import io.javalin.community.routing.Route.AFTER
import io.javalin.community.routing.Route.AFTER_MATCHED
import io.javalin.community.routing.Route.BEFORE
import io.javalin.community.routing.Route.BEFORE_MATCHED
import io.javalin.community.routing.Route.DELETE
import io.javalin.community.routing.Route.GET
import io.javalin.community.routing.Route.HEAD
import io.javalin.community.routing.Route.OPTIONS
import io.javalin.community.routing.Route.PATCH
import io.javalin.community.routing.Route.POST
import io.javalin.community.routing.Route.PUT
import io.javalin.community.routing.Routes
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import kotlin.reflect.KClass

/* Dsl setup */

interface RoutingDslFactory<
    CONFIG : RoutingDslConfiguration<ROUTE, CONTEXT, RESPONSE>,
    ROUTE : DslRoute<CONTEXT, RESPONSE>,
    CONTEXT,
    RESPONSE : Any
> {

    fun createConfiguration(): CONFIG

    fun createHandler(route: ROUTE): Handler

    fun createExceptionHandler(handler: DslExceptionHandler<CONTEXT, Exception, RESPONSE>): ExceptionHandler<Exception>

}

open class RoutingDslConfiguration<ROUTE : DslRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> {

    internal val routes = mutableSetOf<ROUTE>()
    internal val exceptionHandlers = mutableMapOf<KClass<out Exception>, DslExceptionHandler<CONTEXT, Exception, RESPONSE>>()

    // Active path scopes, used as prefix for nested routes.
    private val pathStack = mutableListOf<String>()

    fun get(path: String, handler: CONTEXT.() -> RESPONSE) = route(GET, path, handler)
    fun get(handler: CONTEXT.() -> RESPONSE) = get("", handler)
    fun post(path: String, handler: CONTEXT.() -> RESPONSE) = route(POST, path, handler)
    fun post(handler: CONTEXT.() -> RESPONSE) = post("", handler)
    fun put(path: String, handler: CONTEXT.() -> RESPONSE) = route(PUT, path, handler)
    fun put(handler: CONTEXT.() -> RESPONSE) = put("", handler)
    fun delete(path: String, handler: CONTEXT.() -> RESPONSE) = route(DELETE, path, handler)
    fun delete(handler: CONTEXT.() -> RESPONSE) = delete("", handler)
    fun patch(path: String, handler: CONTEXT.() -> RESPONSE)  = route(PATCH, path, handler)
    fun patch(handler: CONTEXT.() -> RESPONSE) = patch("", handler)
    fun head(path: String, handler: CONTEXT.() -> RESPONSE) = route(HEAD, path, handler)
    fun head(handler: CONTEXT.() -> RESPONSE) = head("", handler)
    fun options(path: String, handler: CONTEXT.() -> RESPONSE) = route(OPTIONS, path, handler)
    fun options(handler: CONTEXT.() -> RESPONSE) = options("", handler)

    // Empty filter paths become wildcards only inside path scopes.
    fun before(path: String = "", handler: CONTEXT.() -> RESPONSE) = route(BEFORE, scopedFilterPath(path), handler)
    fun beforeMatched(path: String = "", handler: CONTEXT.() -> RESPONSE) = route(BEFORE_MATCHED, scopedFilterPath(path), handler)
    fun after(path: String = "", handler: CONTEXT.() -> RESPONSE) = route(AFTER, scopedFilterPath(path), handler)
    fun afterMatched(path: String = "", handler: CONTEXT.() -> RESPONSE) = route(AFTER_MATCHED, scopedFilterPath(path), handler)

    // Runs block with the given path prefix.
    fun path(path: String, setup: RoutingDslConfiguration<ROUTE, CONTEXT, RESPONSE>.() -> Unit) {
        pathStack.add(normalizeScopePath(path))
        try {
            setup()
        } finally {
            pathStack.removeAt(pathStack.lastIndex)
        }
    }

    fun register(vararg containers: DslContainer<ROUTE, CONTEXT, RESPONSE>) {
        containers.forEach { container ->
            register(container.routes())
            
            container.exceptionHandlers().forEach { 
                exception(it.type, it.handler) 
            }
        }
    }

    fun register(routesToAdd: Collection<DslRoute<CONTEXT, RESPONSE>>) =
        routesToAdd.forEach { route ->
            val routeToAdd = if (pathStack.isEmpty()) route else DefaultDslRoute(
                method = route.method,
                path = prefixedPath(route.method, route.path),
                version = route.version,
                handler = route.handler
            )

            @Suppress("UNCHECKED_CAST")
            routes.add(routeToAdd as ROUTE)
        }

    fun route(method: Route, path: String, handler: CONTEXT.() -> RESPONSE) {
        @Suppress("UNCHECKED_CAST")
        routes.add(
            DefaultDslRoute(
                method = method,
                path = prefixedPath(method, path),
                handler = handler
            )
                as ROUTE
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun <EXCEPTION : Exception> exception(type: KClass<EXCEPTION>, handler: DslExceptionHandler<CONTEXT, EXCEPTION, RESPONSE>) {
        if (exceptionHandlers.containsKey(type)) {
            throw IllegalArgumentException("Exception handler for type ${type.simpleName} is already registered")
        }
        exceptionHandlers[type] = handler as DslExceptionHandler<CONTEXT, Throwable, RESPONSE>
    }

    // Prefixes path with active scopes; keeps top-level filter empty paths as-is.
    private fun prefixedPath(method: Route, path: String): String {
        if (path.isEmpty() && !method.isHttpMethod && pathStack.isEmpty()) {
            return path
        }

        if (path == "*") {
            return if (pathStack.isNotEmpty()) "${pathStack.joinToString("")}/*" else "*"
        }

        val prefixedPath = pathStack.joinToString("") + normalizeRoutePath(path)
        return prefixedPath.ifBlank { "/" }
    }

    private fun normalizeScopePath(path: String): String {
        val normalized = if (path.startsWith("/")) path else "/$path"
        return normalized.removeSuffix("/").takeUnless { it.isEmpty() } ?: ""
    }

    private fun normalizeRoutePath(path: String): String {
        return when {
            path == "*" -> path
            path.startsWith("/") || path.isEmpty() -> path
            else -> "/$path"
        }
    }

    // Empty filter path -> wildcard only in nested scopes.
    private fun scopedFilterPath(path: String): String =
        if (path.isEmpty() && pathStack.isNotEmpty()) "*" else path

}

interface DslContainer<ROUTE : DslRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> : Routes<ROUTE, CONTEXT, RESPONSE> {

    fun exceptionHandlers(): Collection<DslException<CONTEXT, Exception, RESPONSE>> = emptySet()

    fun route(method: Route, path: String, handler: CONTEXT.() -> RESPONSE): DslRoute<CONTEXT, RESPONSE> =
        DefaultDslRoute(
            method = method,
            path = path,
            handler = handler
        )

    @Suppress("UNCHECKED_CAST")
    fun <EXCEPTION : Exception> exceptionHandler(type: KClass<EXCEPTION>, handler: DslExceptionHandler<CONTEXT, EXCEPTION, RESPONSE>): DslException<CONTEXT, Exception, RESPONSE> =
        DefaultDslException(
            type = type as KClass<Exception>,
            handler = handler as DslExceptionHandler<CONTEXT, Exception, RESPONSE>
        )

}
