package io.javalin.community.routing.annotations

import io.javalin.community.routing.Route
import io.javalin.community.routing.dsl.DefaultDslRoute
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.http.Context
import io.javalin.validation.Validator
import java.lang.reflect.Parameter

object ReflectiveEndpointLoader {

    private val repeatedPathSeparatorRegex = Regex("/+")

    fun loadRoutesFromEndpoint(endpoint: Any): List<DslRoute<Context, Unit>> {
        val endpointClass = endpoint::class.java

        val endpointPath = endpointClass.getAnnotation(Endpoints::class.java)
            ?.value
            ?: throw IllegalArgumentException("Endpoint class must be annotated with @Endpoints")

        val endpointRoutes = mutableListOf<DslRoute<Context, Unit>>()

        endpointClass.declaredMethods.forEach { method ->
            val (httpMethod, path, async) = when {
                method.isAnnotationPresent(Before::class.java) -> method.getAnnotation(Before::class.java).let { Triple(Route.BEFORE, it.value, it.async) }
                method.isAnnotationPresent(After::class.java) -> method.getAnnotation(After::class.java).let { Triple(Route.AFTER, it.value, it.async) }
                method.isAnnotationPresent(Get::class.java) -> method.getAnnotation(Get::class.java).let { Triple(Route.GET, it.value, it.async) }
                method.isAnnotationPresent(Put::class.java) -> method.getAnnotation(Put::class.java).let { Triple(Route.PUT, it.value, it.async) }
                method.isAnnotationPresent(Post::class.java) -> method.getAnnotation(Post::class.java).let { Triple(Route.POST, it.value, it.async) }
                method.isAnnotationPresent(Delete::class.java) -> method.getAnnotation(Delete::class.java).let { Triple(Route.DELETE, it.value, it.async) }
                method.isAnnotationPresent(Head::class.java) -> method.getAnnotation(Head::class.java).let { Triple(Route.HEAD, it.value, it.async) }
                method.isAnnotationPresent(Patch::class.java) -> method.getAnnotation(Patch::class.java).let { Triple(Route.PATCH, it.value, it.async) }
                method.isAnnotationPresent(Options::class.java) -> method.getAnnotation(Options::class.java).let { Triple(Route.OPTIONS, it.value, it.async) }
                else -> return@forEach
            }

            require(method.trySetAccessible()) {
                "Unable to access method $method in class $endpointClass"
            }

            val argumentSuppliers = method.parameters
                .map { createArgumentSupplier(it) ?: throw IllegalArgumentException("Unsupported parameter type: $it") }

            val route = DefaultDslRoute<Context, Unit>(
                method = httpMethod,
                path = ("/$endpointPath/$path").replace(repeatedPathSeparatorRegex, "/"),
                handler = {
                    val arguments = argumentSuppliers
                        .map { it(this) }
                        .toTypedArray()

                    when (async) {
                        true -> async { method.invoke(endpoint, *arguments) }
                        else -> method.invoke(endpoint, *arguments)
                    }
                }
            )

            endpointRoutes.add(route)
        }

        return endpointRoutes
    }

    private fun createArgumentSupplier(parameter: Parameter): ((Context) -> Any?)? =
        with (parameter) {
            when {
                type.isAssignableFrom(Context::class.java) -> return { ctx ->
                    ctx
                }
                isAnnotationPresent(Param::class.java) -> return { ctx ->
                    getAnnotation(Param::class.java)
                        .value
                        .ifEmpty { name }
                        .let { ctx.pathParamAsClass(it, type) }
                        .get()
                }
                isAnnotationPresent(Header::class.java) -> return { ctx ->
                    getAnnotation(Header::class.java)
                        .value
                        .ifEmpty { name }
                        .let { ctx.headerAsClass(it, type) }
                        .get()
                }
                isAnnotationPresent(Query::class.java) -> return { ctx ->
                    getAnnotation(Query::class.java)
                        .value
                        .ifEmpty { name }
                        .let { ctx.queryParamAsClass(it, type) }
                        .get()
                }
                isAnnotationPresent(Form::class.java) -> return { ctx ->
                    getAnnotation(Form::class.java)
                        .value
                        .ifEmpty { name }
                        .let { ctx.formParamAsClass(it, type) }
                        .get()
                }
                isAnnotationPresent(Cookie::class.java) -> return { ctx ->
                    getAnnotation(Cookie::class.java)
                        .value
                        .ifEmpty { name }
                        .let { Validator.create(type, ctx.cookie(it), it) }
                        .get()
                }
                isAnnotationPresent(Body::class.java) -> return { ctx ->
                    ctx.bodyAsClass(parameter.parameterizedType)
                }
                else -> return null
            }
        }

}