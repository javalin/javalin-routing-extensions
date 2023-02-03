package io.javalin.community.routing.annotations

import io.javalin.community.routing.RouteMethod
import io.javalin.community.routing.dsl.DslRoute
import io.javalin.http.Context
import java.lang.reflect.Parameter

object ReflectiveEndpointLoader {

    fun loadRoutesFromEndpoint(endpoint: Any): List<DslRoute<Context, Unit>> {
        val endpointClass = endpoint::class.java

        val endpointPath = endpointClass.getAnnotation(Endpoints::class.java)
            ?.value
            ?: throw IllegalArgumentException("Endpoint class must be annotated with @Endpoints")

        val endpointRoutes = mutableListOf<DslRoute<Context, Unit>>()

        endpointClass.declaredMethods.forEach { method ->
            val (httpMethod, path) = when {
                method.isAnnotationPresent(Before::class.java) -> RouteMethod.GET to method.getAnnotation(Before::class.java).value
                method.isAnnotationPresent(After::class.java) -> RouteMethod.GET to method.getAnnotation(After::class.java).value
                method.isAnnotationPresent(Get::class.java) -> RouteMethod.GET to method.getAnnotation(Get::class.java).value
                method.isAnnotationPresent(Post::class.java) -> RouteMethod.POST to method.getAnnotation(Post::class.java).value
                method.isAnnotationPresent(Put::class.java) -> RouteMethod.POST to method.getAnnotation(Put::class.java).value
                method.isAnnotationPresent(Delete::class.java) -> RouteMethod.POST to method.getAnnotation(Delete::class.java).value
                method.isAnnotationPresent(Options::class.java) -> RouteMethod.POST to method.getAnnotation(Options::class.java).value
                method.isAnnotationPresent(Head::class.java) -> RouteMethod.POST to method.getAnnotation(Head::class.java).value
                method.isAnnotationPresent(Patch::class.java) -> RouteMethod.POST to method.getAnnotation(Patch::class.java).value
                else -> return@forEach
            }

            require(method.trySetAccessible()) { "Unable to access method $method in class $endpointClass" }

            val argumentSuppliers = method.parameters
                .map { createArgumentSupplier(it) ?: throw IllegalArgumentException("Unsupported parameter type: $it") }

            val route = DslRoute<Context, Unit>(
                method = httpMethod,
                path = endpointPath + path,
                handler = {
                    val arguments = argumentSuppliers
                        .map { it(this) }
                        .toTypedArray()

                    method.invoke(endpoint, *arguments)
                }
            )

            endpointRoutes.add(route)
        }

        return endpointRoutes
    }

    private fun createArgumentSupplier(parameter: Parameter): ((Context) -> Any?)? =
        with (parameter) {
            when {
                type.isAssignableFrom(Context::class.java) -> return { it }
                isAnnotationPresent(Param::class.java) -> return { it.pathParam(getAnnotation(Param::class.java).value.ifEmpty { name }) }
                isAnnotationPresent(Header::class.java) -> return { it.header(getAnnotation(Header::class.java).value.ifEmpty { name }) }
                isAnnotationPresent(Query::class.java) -> return { it.queryParam(getAnnotation(Query::class.java).value.ifEmpty { name }) }
                isAnnotationPresent(Body::class.java) -> return { it.bodyAsClass(parameter.parameterizedType) }
                else -> return null
            }
        }

}