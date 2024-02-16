package io.javalin.community.routing.annotations

import io.javalin.community.routing.Route
import io.javalin.community.routing.Route.BEFORE_MATCHED
import io.javalin.community.routing.dsl.DefaultDslException
import io.javalin.community.routing.dsl.DefaultDslRoute
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.validation.Validation
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import kotlin.reflect.KClass

typealias AnnotatedRoute = DefaultDslRoute<Context, Unit>
typealias AnnotatedException = DefaultDslException<Context, Exception, Unit>

internal class ReflectiveEndpointLoader(
    private val resultHandlers: Map<Class<*>, HandlerResultConsumer<*>>
) {

    private val repeatedPathSeparatorRegex = Regex("/+")

    fun loadRoutesFromEndpoint(endpoint: Any): List<AnnotatedRoute> {
        val endpointClass = endpoint::class.java

        val endpointPath = endpointClass.getAnnotation<Endpoints>()
            ?.value
            ?: ""

        val endpointRoutes = mutableListOf<AnnotatedRoute>()

        endpointClass.declaredMethods.forEach { method ->
            val (httpMethod, path, async) = when {
                method.isAnnotationPresent<Before>() -> method.getAnnotation<Before>()!!.let { Triple(Route.BEFORE, it.value, it.async) }
                method.isAnnotationPresent<BeforeMatched>() -> method.getAnnotation<BeforeMatched>()!!.let { Triple(BEFORE_MATCHED, it.value, it.async) }
                method.isAnnotationPresent<After>() -> method.getAnnotation<After>()!!.let { Triple(Route.AFTER, it.value, it.async) }
                method.isAnnotationPresent<AfterMatched>() -> method.getAnnotation<AfterMatched>()!!.let { Triple(Route.AFTER_MATCHED, it.value, it.async) }
                method.isAnnotationPresent<Get>() -> method.getAnnotation<Get>()!!.let { Triple(Route.GET, it.value, it.async) }
                method.isAnnotationPresent<Put>() -> method.getAnnotation<Put>()!!.let { Triple(Route.PUT, it.value, it.async) }
                method.isAnnotationPresent<Post>() -> method.getAnnotation<Post>()!!.let { Triple(Route.POST, it.value, it.async) }
                method.isAnnotationPresent<Delete>() -> method.getAnnotation<Delete>()!!.let { Triple(Route.DELETE, it.value, it.async) }
                method.isAnnotationPresent<Head>() -> method.getAnnotation<Head>()!!.let { Triple(Route.HEAD, it.value, it.async) }
                method.isAnnotationPresent<Patch>() -> method.getAnnotation<Patch>()!!.let { Triple(Route.PATCH, it.value, it.async) }
                method.isAnnotationPresent<Options>() -> method.getAnnotation<Options>()!!.let { Triple(Route.OPTIONS, it.value, it.async) }
                else -> return@forEach
            }

            require(method.trySetAccessible()) {
                "Unable to access method $method in class $endpointClass"
            }

            val argumentSuppliers = method.parameters.map {
                createArgumentSupplier<Unit>(it) ?: throw IllegalArgumentException("Unsupported parameter type: $it")
            }

            val status = method.getAnnotation<Status>()
            val resultHandler = findResultHandler(method)

            val route = AnnotatedRoute(
                method = httpMethod,
                path = ("/$endpointPath/$path").replace(repeatedPathSeparatorRegex, "/"),
                version = method.getAnnotation<Version>()?.value,
                handler = {
                    val arguments = argumentSuppliers
                        .map { it(this, Unit) }
                        .toTypedArray()

                    when (async) {
                        true -> async { invokeAndUnwrapIfErrored(method, endpoint, arguments, ctx = this, status = status, resultHandler = resultHandler) }
                        else -> invokeAndUnwrapIfErrored(method, endpoint, arguments, ctx = this, status = status, resultHandler = resultHandler)
                    }
                }
            )

            endpointRoutes.add(route)
        }

        return endpointRoutes
    }

    @Suppress("UNCHECKED_CAST")
    fun loadExceptionHandlers(endpoint: Any): List<AnnotatedException> {
        val endpointClass = endpoint::class.java
        val dslExceptions = mutableListOf<AnnotatedException>()

        endpointClass.declaredMethods.forEach { method ->
            val exceptionHandlerAnnotation = method.getAnnotation<ExceptionHandler>() ?: return@forEach

            require(method.trySetAccessible()) {
                "Unable to access method $method in class $endpointClass"
            }

            val argumentSuppliers = method.parameters.map {
                createArgumentSupplier<Exception>(it) ?: throw IllegalArgumentException("Unsupported parameter type: $it")
            }

            val status = method.getAnnotation<Status>()
            val resultHandler = findResultHandler(method)

            val dslException = AnnotatedException(
                type = exceptionHandlerAnnotation.value as KClass<Exception>,
                handler = { exception ->
                    val arguments = argumentSuppliers
                        .map { it(this, exception) }
                        .toTypedArray()

                    invokeAndUnwrapIfErrored(
                        method = method,
                        instance = endpoint,
                        arguments = arguments,
                        status = status,
                        ctx = this,
                        resultHandler = resultHandler
                    )
                }
            )

            dslExceptions.add(dslException)
        }

        return dslExceptions
    }

    @Suppress("UNCHECKED_CAST")
    private fun findResultHandler(method: Method): HandlerResultConsumer<Any?> =
        resultHandlers.asSequence()
            .filter { it.key.isAssignableFrom(method.returnType) }
            .sortedWith { a, b ->
                when {
                    a.key.isAssignableFrom(b.key) -> -1
                    b.key.isAssignableFrom(a.key) -> 1
                    else -> throw IllegalStateException("Unable to determine handler for type ${method.returnType}. Found two matching handlers: ${a.key} and ${b.key}")
                }
            }
            .toList()
            .lastOrNull()
            ?.value as? HandlerResultConsumer<Any?>
            ?: throw IllegalStateException("Unsupported return type: ${method.returnType}")

    private fun invokeAndUnwrapIfErrored(
        method: Method,
        instance: Any,
        arguments: Array<Any?>,
        status: Status?,
        ctx: Context,
        resultHandler: HandlerResultConsumer<Any?>
    ): Any =
        try {
            val result = method.invoke(instance, *arguments)
            status
                ?.takeIf { it.success != HttpStatus.UNKNOWN }
                ?.let { ctx.status(it.success) }
            resultHandler.handle(ctx, result)
        } catch (reflectionException: ReflectiveOperationException) {
            status
                ?.takeIf { it.error != HttpStatus.UNKNOWN }
                ?.let { ctx.status(it.error) }
            throw reflectionException.cause ?: reflectionException
        }

    private inline fun <reified CUSTOM : Any> createArgumentSupplier(
        parameter: Parameter,
        noinline custom: (Context, CUSTOM) -> Any? = { _, self -> self }
    ): ((Context, CUSTOM) -> Any?)? =
        with (parameter) {
            when {
                CUSTOM::class.java.isAssignableFrom(type) ->
                    custom
                type.isAssignableFrom(Context::class.java) -> { ctx, _ ->
                    ctx
                }
                isAnnotationPresent<Param>() -> { ctx, _ ->
                    getAnnotationOrThrow<Param>()
                        .value
                        .ifEmpty { name }
                        .let { ctx.pathParamAsClass(it, type) }
                        .get()
                }
                isAnnotationPresent<Header>() -> { ctx, _ ->
                    getAnnotationOrThrow<Header>()
                        .value
                        .ifEmpty { name }
                        .let { ctx.headerAsClass(it, type) }
                        .get()
                }
                isAnnotationPresent<Query>() -> { ctx, _ ->
                    getAnnotationOrThrow<Query>()
                        .value
                        .ifEmpty { name }
                        .let { ctx.queryParamAsClass(it, type) }
                        .get()
                }
                isAnnotationPresent<Form>() -> { ctx, _ ->
                    getAnnotationOrThrow<Form>()
                        .value
                        .ifEmpty { name }
                        .let { ctx.formParamAsClass(it, type) }
                        .get()
                }
                isAnnotationPresent<Cookie>() -> { ctx, _ ->
                    getAnnotationOrThrow<Cookie>()
                        .value
                        .ifEmpty { name }
                        .let { Validation().validator(it, type, ctx.cookie(it)) }
                        .get()
                }
                isAnnotationPresent<Body>() -> { ctx, _ ->
                    ctx.bodyAsClass(parameter.parameterizedType)
                }
                else -> null
            }
        }

    private inline fun <reified A : Annotation> AnnotatedElement.isAnnotationPresent(): Boolean =
        isAnnotationPresent(A::class.java)

    private inline fun <reified A : Annotation> AnnotatedElement.getAnnotationOrThrow(): A =
        getAnnotation(A::class.java) ?: throw IllegalStateException("Annotation ${A::class.java.name} not found")

    private inline fun <reified A : Annotation> AnnotatedElement.getAnnotation(): A? =
        getAnnotation(A::class.java)

}