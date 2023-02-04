package io.javalin.community.routing.coroutines.servlet

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.CoroutineContext.Key

class DefaultUncaughtExceptionHandler(
    private val errorConsumer: (Any, Throwable) -> Unit,
    override val key: Key<*> = CoroutineExceptionHandler
) : CoroutineExceptionHandler {

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is CancellationException -> return
            else -> {
                val coroutineName = context[CoroutineName] ?: context.toString()
                errorConsumer(coroutineName, exception)
            }
        }
    }

}