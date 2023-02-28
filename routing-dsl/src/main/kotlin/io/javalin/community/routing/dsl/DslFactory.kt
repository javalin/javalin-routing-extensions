package io.javalin.community.routing.dsl

import io.javalin.community.routing.Route
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

    fun get(path: String, handler: CONTEXT.() -> RESPONSE) =
        addRoute(Route.GET, path, handler)

    fun post(path: String, handler: CONTEXT.() -> RESPONSE) =
        addRoute(Route.POST, path, handler)

    fun put(path: String, handler: CONTEXT.() -> RESPONSE) =
        addRoute(Route.PUT, path, handler)

    fun delete(path: String, handler: CONTEXT.() -> RESPONSE) =
        addRoute(Route.DELETE, path, handler)

    fun patch(path: String, handler: CONTEXT.() -> RESPONSE)  =
        addRoute(Route.PATCH, path, handler)

    fun head(path: String, handler: CONTEXT.() -> RESPONSE) =
        addRoute(Route.HEAD, path, handler)

    fun options(path: String, handler: CONTEXT.() -> RESPONSE) =
        addRoute(Route.OPTIONS, path, handler)

    fun before(path: String = "", handler: CONTEXT.() -> RESPONSE) =
        addRoute(Route.BEFORE, path, handler)

    fun after(path: String = "", handler: CONTEXT.() -> RESPONSE) =
        addRoute(Route.AFTER, path, handler)

    @Suppress("UNCHECKED_CAST")
    fun addRoutes(routesToAdd: Collection<DslRoute<CONTEXT, RESPONSE>>) {
        routesToAdd.forEach {
            routes.add(it as ROUTE)
        }
    }

    fun addRoute(method: Route, path: String, handler: CONTEXT.() -> RESPONSE) {
        addRoutes(
            listOf(
                DefaultDslRoute(
                    method = method,
                    path = path,
                    handler = handler
                )
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun <EXCEPTION : Exception> exception(type: KClass<EXCEPTION>, handler: DslExceptionHandler<CONTEXT, EXCEPTION, RESPONSE>) {
        if (exceptionHandlers.containsKey(type)) {
            throw IllegalArgumentException("Exception handler for type ${type.simpleName} is already registered")
        }

        exceptionHandlers[type] = handler as DslExceptionHandler<CONTEXT, Throwable, RESPONSE>
    }

}

interface DslContainer<ROUTE : DslRoute<CONTEXT, RESPONSE>, CONTEXT, RESPONSE : Any> : Routes<ROUTE, CONTEXT, RESPONSE> {

    fun exceptionHandlers(): Collection<DslException<CONTEXT, Exception, RESPONSE>> = emptySet()

    fun route(path: String, method: Route, handler: CONTEXT.() -> RESPONSE): DslRoute<CONTEXT, RESPONSE> =
        DefaultDslRoute(
            path = path,
            method = method,
            handler = handler
        )

    @Suppress("UNCHECKED_CAST")
    fun <EXCEPTION : Exception> exceptionHandler(type: KClass<EXCEPTION>, handler: DslExceptionHandler<CONTEXT, EXCEPTION, RESPONSE>): DslException<CONTEXT, Exception, RESPONSE> =
        DefaultDslException(
            type = type as KClass<Exception>,
            handler = handler as DslExceptionHandler<CONTEXT, Exception, RESPONSE>
        )

}
