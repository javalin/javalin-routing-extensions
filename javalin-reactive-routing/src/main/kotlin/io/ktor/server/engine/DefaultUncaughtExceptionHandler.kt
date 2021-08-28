package io.ktor.server.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlin.coroutines.CoroutineContext

typealias CoroutineNameRepresentation = Any

class DefaultUncaughtExceptionHandler(
    private val errorConsumer: (CoroutineNameRepresentation, Throwable) -> Unit
) : CoroutineExceptionHandler {

    override val key: CoroutineContext.Key<*> = CoroutineExceptionHandler.Key

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
