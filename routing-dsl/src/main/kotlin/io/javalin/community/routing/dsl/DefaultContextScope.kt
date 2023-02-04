package io.javalin.community.routing.dsl

import io.javalin.community.routing.Route
import io.javalin.http.Context
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.javaType

interface DefaultContextScope {
    val ctx: Context
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Path(
    val path: String
)

open class DefaultContextScopeConfiguration<
    ROUTE : DslRoute<CONTEXT, RESPONSE>,
    CONTEXT : DefaultContextScope,
    RESPONSE : Any
> : RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>() {

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified PATH : Any> RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>.method(
        method: Route,
        crossinline handler: (CONTEXT, PATH) -> RESPONSE
    ) {
        val path = PATH::class.findAnnotation<Path>()
            ?.path
            ?: throw IllegalArgumentException("@Path annotation not found")

        val primaryConstructor = PATH::class.primaryConstructor

        primaryConstructor
            ?.parameters
            ?.forEach {
                require(path.contains("{${it.name}") || path.contains("<${it.name}>")) {
                    "Path parameter '${it.name}' not found in path $path (path type: ${PATH::class})"
                }
            }

        val standardHandler: CONTEXT.() -> RESPONSE = {
            val pathInstance = primaryConstructor
                ?.parameters
                ?.associateWith {
                    ctx.pathParamAsClass(
                        it.name ?: throw IllegalStateException("Unknown parameter name in class ${PATH::class}"),
                        it.type.javaType as Class<*>
                    ).get()
                }
                ?.let { primaryConstructor.callBy(it) }
                ?: PATH::class.createInstance()

            handler(this, pathInstance)
        }

        when (method) {
            Route.GET -> get(path, standardHandler)
            Route.POST -> post(path, standardHandler)
            Route.PUT -> put(path, standardHandler)
            Route.DELETE -> delete(path, standardHandler)
            Route.PATCH -> patch(path, standardHandler)
            Route.HEAD -> head(path, standardHandler)
            Route.OPTIONS -> options(path, standardHandler)
            Route.BEFORE -> before(path, standardHandler)
            Route.AFTER -> after(path, standardHandler)
        }
    }

    inline fun <reified PATH : Any> RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>.get(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(Route.GET, handler)

    inline fun <reified PATH : Any> RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>.post(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(Route.POST, handler)

    inline fun <reified PATH : Any> RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>.put(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(Route.PUT, handler)

    inline fun <reified PATH : Any> RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>.delete(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(Route.DELETE, handler)

    inline fun <reified PATH : Any> RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>.patch(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(Route.PATCH, handler)

    inline fun <reified PATH : Any> RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>.head(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(Route.HEAD, handler)

    inline fun <reified PATH : Any> RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>.options(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(Route.OPTIONS, handler)

    inline fun <reified PATH : Any> RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>.before(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(Route.BEFORE, handler)

    inline fun <reified PATH : Any> RoutingConfiguration<ROUTE, CONTEXT, RESPONSE>.after(crossinline handler: CONTEXT.(PATH) -> RESPONSE) =
        method(Route.AFTER, handler)

}
