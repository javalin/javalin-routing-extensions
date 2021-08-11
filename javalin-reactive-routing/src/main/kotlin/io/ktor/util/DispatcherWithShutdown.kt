package io.ktor.util

/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import io.ktor.util.DispatcherWithShutdown.ShutdownPhase.Completed
import io.ktor.util.DispatcherWithShutdown.ShutdownPhase.Graceful
import io.ktor.util.DispatcherWithShutdown.ShutdownPhase.None
import kotlinx.coroutines.*
import java.util.concurrent.*
import kotlin.coroutines.*

/**
 * Specialized dispatcher useful for graceful shutdown
 */
class DispatcherWithShutdown(delegate: CoroutineDispatcher) : CoroutineDispatcher() {
    private var delegate: CoroutineDispatcher? = delegate

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

    private enum class ShutdownPhase {
        None, Graceful, Completed
    }
}
