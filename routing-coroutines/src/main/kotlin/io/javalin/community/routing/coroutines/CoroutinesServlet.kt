package io.javalin.community.routing.coroutines

import io.javalin.community.routing.coroutines.ktor.CoroutineNameRepresentation
import io.javalin.community.routing.coroutines.ktor.DefaultUncaughtExceptionHandler
import io.javalin.http.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.EmptyCoroutineContext

open class CoroutinesServlet<CONTEXT, RESPONSE>(
    name: String = "javalin-rfc-scope:reactive-routing",
    protected val coroutinesEnabled: Boolean = true,
    errorConsumer: (CoroutineNameRepresentation, Throwable) -> Unit,
    protected val dispatcher: CoroutineDispatcher,
    protected val syncHandler: suspend (Context, AsyncRoute<CONTEXT, RESPONSE>) -> RESPONSE,
    protected val asyncHandler: suspend (Context, AsyncRoute<CONTEXT, RESPONSE>, CompletableFuture<RESPONSE>) -> RESPONSE,
    protected val responseConsumer: (suspend (Context, RESPONSE) -> Unit)? = null
) {

    protected val coroutineName: CoroutineName = CoroutineName(name)
    protected val exceptionHandler = DefaultUncaughtExceptionHandler(errorConsumer)
    protected val scope = JavalinCoroutineScope(EmptyCoroutineContext, exceptionHandler)
    protected val id = AtomicLong()
    protected val finished = AtomicLong()

    fun handle(ctx: Context, route: AsyncRoute<CONTEXT, RESPONSE>) {
        id.incrementAndGet()

        when {
            coroutinesEnabled && route.async -> {
                ctx.future {
                    val result = CompletableFuture<RESPONSE>()

                    scope.launch(dispatcher + coroutineName) {
                        runCatching { asyncHandler(ctx, route, result) }
                            .map {
                                responseConsumer?.invoke(ctx, it)
                                result.complete(it)
                            }
                            .onFailure { result.completeExceptionally(it) }
                        finished.incrementAndGet()
                    }

                    result
                }
            }
            else -> runBlocking {
                val response = syncHandler(ctx, route)
                responseConsumer?.invoke(ctx, response)
                finished.incrementAndGet()
            }
        }
    }

    fun countFinishedTasks(): Long =
        finished.get()

    fun countExecutedTasks(): Long =
        id.get()

}