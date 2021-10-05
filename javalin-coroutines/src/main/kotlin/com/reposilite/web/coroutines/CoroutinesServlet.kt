package com.reposilite.web.coroutines

import com.reposilite.web.coroutines.ktor.CoroutineNameRepresentation
import com.reposilite.web.coroutines.ktor.DefaultUncaughtExceptionHandler
import com.reposilite.web.routing.Route
import io.javalin.http.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.EmptyCoroutineContext

open class CoroutinesServlet<CONTEXT, RESPONSE : Any>(
    name: String = "javalin-rfc-scope:reactive-routing",
    protected val coroutinesEnabled: Boolean = true,
    errorConsumer: (CoroutineNameRepresentation, Throwable) -> Unit,
    protected val dispatcher: CoroutineDispatcher,
    protected val syncHandler: suspend (Context, Route<CONTEXT, RESPONSE>) -> RESPONSE,
    protected val asyncHandler: suspend (Context, Route<CONTEXT, RESPONSE>, CompletableFuture<RESPONSE>) -> RESPONSE
) {

    protected val coroutineName: CoroutineName = CoroutineName(name)
    protected val exceptionHandler = DefaultUncaughtExceptionHandler(errorConsumer)
    protected val scope = JavalinCoroutineScope(EmptyCoroutineContext, exceptionHandler)
    protected val id = AtomicLong()
    protected val finished = AtomicLong()

    fun handle(ctx: Context, route: Route<CONTEXT, RESPONSE>) {
        id.incrementAndGet()

        if (coroutinesEnabled && route.async) {
            val result = CompletableFuture<RESPONSE>()
            ctx.future(result) { /* Disable default processing with empty body */ }

            scope.launch(dispatcher + coroutineName) {
                runCatching {
                    asyncHandler(ctx, route, result)
                }.onFailure {
                    result.completeExceptionally(it)
                }
                finished.incrementAndGet()
            }
        } else {
            runBlocking {
                syncHandler(ctx, route)
            }
            finished.incrementAndGet()
        }
    }

    fun countFinishedTasks(): Long =
        finished.get()

    fun countExecutedTasks(): Long =
        id.get()

}