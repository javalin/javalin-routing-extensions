package io.javalin.community.routing.dsl.defaults

import io.javalin.community.routing.Route
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.community.routing.dsl.RoutingDslConfiguration
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
> : RoutingDslConfiguration<ROUTE, CONTEXT, RESPONSE>() {

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified PATH : Any> method(method: Route, crossinline handler: (CONTEXT, PATH) -> RESPONSE) {
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
            Route.BEFORE_MATCHED -> beforeMatched(path, standardHandler)
            Route.AFTER -> after(path, standardHandler)
            Route.AFTER_MATCHED -> afterMatched(path, standardHandler)
        }
    }

    inline fun <reified PATH : Any> get(crossinline handler: CONTEXT.(PATH) -> RESPONSE) = method(Route.GET, handler)
    inline fun <reified PATH : Any> post(crossinline handler: CONTEXT.(PATH) -> RESPONSE) = method(Route.POST, handler)
    inline fun <reified PATH : Any> put(crossinline handler: CONTEXT.(PATH) -> RESPONSE) = method(Route.PUT, handler)
    inline fun <reified PATH : Any> delete(crossinline handler: CONTEXT.(PATH) -> RESPONSE) = method(Route.DELETE, handler)
    inline fun <reified PATH : Any> patch(crossinline handler: CONTEXT.(PATH) -> RESPONSE) = method(Route.PATCH, handler)
    inline fun <reified PATH : Any> head(crossinline handler: CONTEXT.(PATH) -> RESPONSE) = method(Route.HEAD, handler)
    inline fun <reified PATH : Any> options(crossinline handler: CONTEXT.(PATH) -> RESPONSE) = method(Route.OPTIONS, handler)
    inline fun <reified PATH : Any> before(crossinline handler: CONTEXT.(PATH) -> RESPONSE) = method(Route.BEFORE, handler)
    inline fun <reified PATH : Any> after(crossinline handler: CONTEXT.(PATH) -> RESPONSE) = method(Route.AFTER, handler)

}
