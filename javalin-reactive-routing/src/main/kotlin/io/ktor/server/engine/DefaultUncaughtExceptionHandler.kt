package io.ktor.server.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import org.slf4j.Logger
import java.io.IOException
import kotlin.coroutines.CoroutineContext

public class DefaultUncaughtExceptionHandler(
    private val logger: () -> Logger
) : CoroutineExceptionHandler {
    public constructor(logger: Logger) : this({ logger })

    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler.Key

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        if (exception is CancellationException) return
        if (exception is IOException) return

        val coroutineName = context[CoroutineName] ?: context.toString()

        logger().error("Unhandled exception caught for $coroutineName", exception)
    }
}
