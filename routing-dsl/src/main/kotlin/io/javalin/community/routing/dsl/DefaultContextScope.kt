package io.javalin.community.routing.dsl

import io.javalin.community.routing.RouteMethod
import io.javalin.http.Context
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.javaType

annotation class Path(val path: String)

interface DefaultContextScope {
    val ctx: Context
}

open class DefaultContextScopeConfiguration<CONTEXT : DefaultContextScope, RESPONSE : Any> : RoutingConfiguration<CONTEXT, RESPONSE>() {

    inline fun <reified PATH : Any> RoutingConfiguration<CONTEXT, RESPONSE>.get(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(RouteMethod.GET, handler)

    inline fun <CONTEXT : DefaultContextScope, RESPONSE, reified PATH : Any> RoutingConfiguration<CONTEXT, RESPONSE>.post(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(RouteMethod.POST, handler)

    inline fun <CONTEXT : DefaultContextScope, RESPONSE, reified PATH : Any> RoutingConfiguration<CONTEXT, RESPONSE>.put(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(RouteMethod.PUT, handler)

    inline fun <CONTEXT : DefaultContextScope, RESPONSE, reified PATH : Any> RoutingConfiguration<CONTEXT, RESPONSE>.delete(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(RouteMethod.DELETE, handler)

    inline fun <CONTEXT : DefaultContextScope, RESPONSE, reified PATH : Any> RoutingConfiguration<CONTEXT, RESPONSE>.patch(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(RouteMethod.PATCH, handler)

    inline fun <CONTEXT : DefaultContextScope, RESPONSE, reified PATH : Any> RoutingConfiguration<CONTEXT, RESPONSE>.head(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(RouteMethod.HEAD, handler)

    inline fun <CONTEXT : DefaultContextScope, RESPONSE, reified PATH : Any> RoutingConfiguration<CONTEXT, RESPONSE>.options(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(RouteMethod.OPTIONS, handler)

    inline fun <CONTEXT : DefaultContextScope, RESPONSE, reified PATH : Any> RoutingConfiguration<CONTEXT, RESPONSE>.before(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(RouteMethod.BEFORE, handler)

    inline fun <CONTEXT : DefaultContextScope, RESPONSE, reified PATH : Any> RoutingConfiguration<CONTEXT, RESPONSE>.after(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(RouteMethod.AFTER, handler)

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <CONTEXT : DefaultContextScope, RESPONSE, reified PATH : Any> RoutingConfiguration<CONTEXT, RESPONSE>.method(
        method: RouteMethod,
        crossinline handler: (CONTEXT, PATH) -> RESPONSE
    ) {
        val path = PATH::class.findAnnotation<Path>()
            ?.path
            ?: throw IllegalArgumentException("@Path annotation not found")

        val primaryConstructor = PATH::class
            .takeIf { it.isData }
            ?.primaryConstructor
            ?: throw IllegalArgumentException("Path must be a data class with primary constructor")

        val standardHandler: CONTEXT.() -> RESPONSE = {
            val pathInstance = primaryConstructor
                .parameters.associateWith {
                    ctx.pathParamAsClass(
                        it.name ?: throw IllegalStateException("Unknown parameter name in class ${PATH::class}"),
                        it.type.javaType as Class<*>
                    ).get()
                }
                .let { primaryConstructor.callBy(it) }

            handler(this, pathInstance)
        }

        when (method) {
            RouteMethod.GET -> get(path, standardHandler)
            RouteMethod.POST -> post(path, standardHandler)
            RouteMethod.PUT -> put(path, standardHandler)
            RouteMethod.DELETE -> delete(path, standardHandler)
            RouteMethod.PATCH -> patch(path, standardHandler)
            RouteMethod.HEAD -> head(path, standardHandler)
            RouteMethod.OPTIONS -> options(path, standardHandler)
            RouteMethod.BEFORE -> before(path, standardHandler)
            RouteMethod.AFTER -> after(path, standardHandler)
        }
    }

}
