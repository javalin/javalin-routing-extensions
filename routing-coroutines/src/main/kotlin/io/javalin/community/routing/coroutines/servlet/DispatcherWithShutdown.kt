package io.javalin.community.routing.coroutines.servlet

import io.javalin.community.routing.coroutines.servlet.DispatcherWithShutdown.ShutdownPhase.Completed
import io.javalin.community.routing.coroutines.servlet.DispatcherWithShutdown.ShutdownPhase.Graceful
import io.javalin.community.routing.coroutines.servlet.DispatcherWithShutdown.ShutdownPhase.None
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlin.coroutines.CoroutineContext

// Source: Ktor

/**
 * Specialized dispatcher useful for graceful shutdown
 */
class DispatcherWithShutdown(delegate: CoroutineDispatcher) : CoroutineDispatcher() {

    private var delegate: CoroutineDispatcher? = delegate

    private enum class ShutdownPhase {
        None, Graceful, Completed
    }

    @Volatile
    private var shutdownPhase = None
    private val shutdownPool = lazy { Executors.newCachedThreadPool() }

    /**
     * Prepare for shutdown so we will not dispatch on [delegate] anymore. It is still possible to
     * dispatch coroutines.
     */
    fun prepareShutdown() {
        shutdownPhase = Graceful
        delegate = null
    }

    /**
     * Complete shutdown. Any further attempts to dispatch anything will fail with [RejectedExecutionException]
     */
    fun completeShutdown() {
        shutdownPhase = Completed
        if (shutdownPool.isInitialized()) shutdownPool.value.shutdown()
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return when (shutdownPhase) {
            None -> delegate?.isDispatchNeeded(context) ?: true
            else -> true
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        when (shutdownPhase) {
            None -> {
                try {
                    delegate?.dispatch(context, block) ?: return dispatch(context, block)
                } catch (rejected: RejectedExecutionException) {
                    if (shutdownPhase != None) return dispatch(context, block)
                    throw rejected
                }
            }
            Graceful -> {
                try {
                    shutdownPool.value.submit(block)
                } catch (rejected: RejectedExecutionException) {
                    shutdownPhase = Completed
                    return dispatch(context, block)
                }
            }
            Completed -> {
                block.run()
            }
        }
    }

}