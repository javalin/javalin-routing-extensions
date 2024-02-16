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

    fun get(path: String, handler: CONTEXT.() -> RESPONSE) = route(GET, path, handler)
    fun post(path: String, handler: CONTEXT.() -> RESPONSE) = route(POST, path, handler)
    fun put(path: String, handler: CONTEXT.() -> RESPONSE) = route(PUT, path, handler)
    fun delete(path: String, handler: CONTEXT.() -> RESPONSE) = route(DELETE, path, handler)
    fun patch(path: String, handler: CONTEXT.() -> RESPONSE)  = route(PATCH, path, handler)
    fun head(path: String, handler: CONTEXT.() -> RESPONSE) = route(HEAD, path, handler)
    fun options(path: String, handler: CONTEXT.() -> RESPONSE) = route(OPTIONS, path, handler)
    fun before(path: String = "", handler: CONTEXT.() -> RESPONSE) = route(BEFORE, path, handler)
    fun beforeMatched(path: String = "", handler: CONTEXT.() -> RESPONSE) = route(BEFORE_MATCHED, path, handler)
    fun after(path: String = "", handler: CONTEXT.() -> RESPONSE) = route(AFTER, path, handler)
    fun afterMatched(path: String = "", handler: CONTEXT.() -> RESPONSE) = route(AFTER_MATCHED, path, handler)

    fun routes(container: DslContainer<ROUTE, CONTEXT, RESPONSE>) {
        routes(container.routes())
        container.exceptionHandlers().forEach { exception(it.type, it.handler) }
    }

    fun routes(routesToAdd: Collection<DslRoute<CONTEXT, RESPONSE>>) = routesToAdd.forEach {
        @Suppress("UNCHECKED_CAST")
        routes.add(it as ROUTE)
    }

    fun route(method: Route, path: String, handler: CONTEXT.() -> RESPONSE) {
        routes(
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
