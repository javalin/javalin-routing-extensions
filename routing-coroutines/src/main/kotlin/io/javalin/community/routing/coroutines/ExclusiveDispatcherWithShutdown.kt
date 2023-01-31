package io.javalin.community.routing.coroutines

import io.javalin.community.routing.coroutines.ktor.DispatcherWithShutdown
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import kotlin.coroutines.CoroutineContext

interface GracefullyShutdownableDispatcher {
    fun prepareShutdown()
    fun completeShutdown()
}

open class ExclusiveDispatcherWithShutdown(private val executorService: ExecutorService) : CoroutineDispatcher(), GracefullyShutdownableDispatcher {

    private val dispatcher = DispatcherWithShutdown(executorService.asCoroutineDispatcher())

    override fun dispatch(context: CoroutineContext, block: Runnable) =
        dispatcher.dispatch(context, block)

    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        dispatcher.isDispatchNeeded(context)

    override fun prepareShutdown() {
        dispatcher.prepareShutdown()
        executorService.shutdown()
    }

    override fun completeShutdown() {
        dispatcher.completeShutdown()
        executorService.shutdownNow()
    }

}