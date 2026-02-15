package io.javalin.community.routing.annotations

import io.javalin.community.routing.Route
import io.javalin.community.routing.Route.BEFORE_MATCHED
import io.javalin.community.routing.dsl.DefaultDslException
import io.javalin.community.routing.dsl.DefaultDslRoute
import io.javalin.event.JavalinLifecycleEvent
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.router.Endpoint
import io.javalin.router.InternalRouter
import io.javalin.util.Util.firstOrNull
import io.javalin.validation.Validation
import io.javalin.validation.Validator
import io.javalin.websocket.WsConfig
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import java.util.function.Consumer
import kotlin.reflect.KClass

typealias AnnotatedRoute = DefaultDslRoute<Context, Unit>
typealias AnnotatedException = DefaultDslException<Context, Exception, Unit>
typealias AnnotatedEvent = () -> Unit

data class AnnotatedWsRoute(val path: String, val wsConfig: Consumer<WsConfig>)

internal class ReflectiveEndpointLoader(
    private val internalRouter: InternalRouter,
    private val resultHandlers: Map<Class<*>, HandlerResultConsumer<*>>
) {

    private val repeatedPathSeparatorRegex = Regex("/+")

    fun loadRoutesFromEndpoint(endpoint: Any): List<AnnotatedRoute> {
        val endpointClass = endpoint::class.java

        val endpointPath = endpointClass.getAnnotation<Endpoints>()
            ?.value
            ?: ""

        val endpointRoutes = mutableListOf<AnnotatedRoute>()

        getAllDeclaredMethods(endpointClass).forEach { method ->
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

            val types = method.genericParameterTypes
            val argumentSuppliers = method.parameters.mapIndexed { idx, parameter ->
                createArgumentSupplier<Unit>(parameter, types[idx]) ?: throw IllegalArgumentException("Unsupported parameter type: $parameter")
            }

            val status = method.getAnnotation<Status>()
            val resultHandler = findResultHandler(method)

            val declaredPath = "/$endpointPath/$path".replace(repeatedPathSeparatorRegex, "/").dropLastWhile { it == '/' }
            val processedPaths = mutableSetOf(declaredPath)

            if (!httpMethod.isHttpMethod && declaredPath.endsWith("*")) {
                processedPaths.add(declaredPath.dropLast(1).dropLastWhile { it == '/' })
            }

            processedPaths.forEach { pathToAdd ->
                endpointRoutes.add(
                    AnnotatedRoute(
                        method = httpMethod,
                        path = pathToAdd,
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
                )
            }
        }

        return endpointRoutes
    }

    @Suppress("UNCHECKED_CAST")
    fun loadExceptionHandlers(endpoint: Any): List<AnnotatedException> {
        val endpointClass = endpoint::class.java
        val dslExceptions = mutableListOf<AnnotatedException>()

        getAllDeclaredMethods(endpointClass).forEach { method ->
            val exceptionHandlerAnnotation = method.getAnnotation<ExceptionHandler>() ?: return@forEach

            require(method.trySetAccessible()) {
                "Unable to access method $method in class $endpointClass"
            }

            val types = method.genericParameterTypes
            val argumentSuppliers = method.parameters.mapIndexed { idx, parameter ->
                createArgumentSupplier<Exception>(parameter, types[idx]) ?: throw IllegalArgumentException("Unsupported parameter type: $parameter")
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

    fun loadEventHandlers(endpoint: Any): Map<JavalinLifecycleEvent, AnnotatedEvent> {
        val endpointClass = endpoint::class.java
        val dslEvents = mutableMapOf<JavalinLifecycleEvent, AnnotatedEvent>()

        getAllDeclaredMethods(endpointClass).forEach { method ->
            val lifecycleEventHandler = method.getAnnotation<LifecycleEventHandler>() ?: return@forEach

            require(method.trySetAccessible()) {
                "Unable to access method $method in class $endpointClass"
            }

            dslEvents[lifecycleEventHandler.lifecycleEvent] = object : AnnotatedEvent {
                override fun invoke() {
                    method.invoke(endpoint)
                }
            }
        }

        return dslEvents
    }

    fun loadWsHandlers(endpoint: Any): List<AnnotatedWsRoute> {
        val endpointClass = endpoint::class.java

        val endpointPath = endpointClass.getAnnotation<Endpoints>()
            ?.value
            ?: ""

        val wsRoutes = mutableListOf<AnnotatedWsRoute>()

        getAllDeclaredMethods(endpointClass).forEach { method ->
            val wsAnnotation = method.getAnnotation<Ws>() ?: return@forEach

            require(method.trySetAccessible()) {
                "Unable to access method $method in class $endpointClass"
            }

            require(WsHandler::class.java.isAssignableFrom(method.returnType)) {
                "Method $method annotated with @Ws must return WsHandler"
            }

            val path = "/$endpointPath/${wsAnnotation.value}".replace(repeatedPathSeparatorRegex, "/").dropLastWhile { it == '/' }

            wsRoutes.add(AnnotatedWsRoute(
                path = path,
                wsConfig = { wsConfig ->
                    val handler = method.invoke(endpoint) as WsHandler
                    wsConfig.onConnect { handler.onConnect(it) }
                    wsConfig.onError { handler.onError(it) }
                    wsConfig.onClose { handler.onClose(it) }
                    wsConfig.onMessage { handler.onMessage(it) }
                    wsConfig.onBinaryMessage { handler.onBinaryMessage(it) }
                }
            ))
        }

        return wsRoutes
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
        parameterType: Type,
        noinline custom: (Context, CUSTOM) -> Any? = { _, self -> self }
    ): ((Context, CUSTOM) -> Any?)? {
        val isOptional = parameter.type.isAssignableFrom(Optional::class.java)

        val expectedType = when {
            isOptional -> (parameterType as ParameterizedType).actualTypeArguments[0]
            else -> parameterType
        }
        val expectedTypeAsClass = expectedType as Class<*>

        return with(parameter) {
            when {
                CUSTOM::class.java.isAssignableFrom(expectedTypeAsClass) -> { ctx, c ->
                    when {
                        isOptional -> Optional.ofNullable(custom(ctx, c))
                        else -> custom(ctx, c)
                    }
                }
                expectedTypeAsClass.isAssignableFrom(Context::class.java) -> { ctx, _ ->
                    ctx
                }
                expectedTypeAsClass.isAssignableFrom(Endpoint::class.java) -> { ctx, _ ->
                    internalRouter
                        .findHttpHandlerEntries(ctx.method(), ctx.path().removePrefix(ctx.contextPath()))
                        .firstOrNull()
                        ?.endpoint
                }
                isAnnotationPresent<Param>() -> { ctx, _ ->
                    getAnnotationOrThrow<Param>()
                        .value
                        .ifEmpty { name }
                        .let { ctx.pathParamAsClass(it, expectedTypeAsClass) }
                        .getValidatorValue(optional = isOptional)
                }
                isAnnotationPresent<Header>() -> { ctx, _ ->
                    getAnnotationOrThrow<Header>()
                        .value
                        .ifEmpty { name }
                        .let { ctx.headerAsClass(it, expectedTypeAsClass) }
                        .getValidatorValue(optional = isOptional)
                }
                isAnnotationPresent<Query>() -> { ctx, _ ->
                    getAnnotationOrThrow<Query>()
                        .value
                        .ifEmpty { name }
                        .let { ctx.queryParamAsClass(it, expectedTypeAsClass) }
                        .getValidatorValue(optional = isOptional)
                }
                isAnnotationPresent<Form>() -> { ctx, _ ->
                    getAnnotationOrThrow<Form>()
                        .value
                        .ifEmpty { name }
                        .let { ctx.formParamAsClass(it, expectedTypeAsClass) }
                        .getValidatorValue(optional = isOptional)
                }
                isAnnotationPresent<Cookie>() -> { ctx, _ ->
                    getAnnotationOrThrow<Cookie>()
                        .value
                        .ifEmpty { name }
                        .let { Validation().validator(it, expectedTypeAsClass, ctx.cookie(it)) }
                        .getValidatorValue(optional = isOptional)
                }
                isAnnotationPresent<Body>() -> { ctx, _ ->
                    ctx.bodyAsClass(parameter.parameterizedType)
                }
                else -> null
            }
        }
    }

    private fun <T : Any> Validator<T?>.getValidatorValue(optional: Boolean): Any =
        when {
            optional -> Optional.ofNullable(this.getOrNull())
            else -> get()
        }

    private inline fun <reified A : Annotation> AnnotatedElement.isAnnotationPresent(): Boolean =
        isAnnotationPresent(A::class.java)

    private inline fun <reified A : Annotation> AnnotatedElement.getAnnotationOrThrow(): A =
        getAnnotation(A::class.java) ?: throw IllegalStateException("Annotation ${A::class.java.name} not found")

    private inline fun <reified A : Annotation> AnnotatedElement.getAnnotation(): A? =
        getAnnotation(A::class.java)

    private fun getAllDeclaredMethods(clazz: Class<*>): Collection<Method> {
        val methods = mutableListOf<Method>()
        var currentClass: Class<*>? = clazz
        while (currentClass?.name != "java.lang.Object") {
            methods.addAll(currentClass?.declaredMethods ?: emptyArray())
            currentClass = currentClass?.superclass
        }
        return methods
    }

}