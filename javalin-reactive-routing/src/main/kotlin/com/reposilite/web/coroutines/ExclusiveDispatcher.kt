package com.reposilite.web.coroutines

import com.reposilite.web.coroutines.ktor.DispatcherWithShutdown
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService
import kotlin.coroutines.CoroutineContext

open class ExclusiveDispatcher(private val executorService: ExecutorService) : CoroutineDispatcher() {

    private val dispatcher = DispatcherWithShutdown(executorService.asCoroutineDispatcher())

    override fun dispatch(context: CoroutineContext, block: Runnable) =
        dispatcher.dispatch(context, block)

    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        dispatcher.isDispatchNeeded(context)

    fun prepareShutdown() {
        dispatcher.prepareShutdown()
        executorService.shutdown()
    }

    fun completeShutdown() {
        dispatcher.completeShutdown()
        executorService.shutdownNow()
    }

}