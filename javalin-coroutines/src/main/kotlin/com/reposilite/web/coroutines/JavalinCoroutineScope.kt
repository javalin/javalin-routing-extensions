package com.reposilite.web.coroutines

import io.javalin.http.util.RateLimitUtil.executor
import io.ktor.server.engine.DefaultUncaughtExceptionHandler
import io.ktor.util.DispatcherWithShutdown
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.Logger
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class JavalinCoroutineScope(
    private val logger: () -> Logger,
    parentContext: CoroutineContext = EmptyCoroutineContext,
    exceptionHandler: CoroutineExceptionHandler = DefaultUncaughtExceptionHandler(logger)
) : CoroutineScope {

    private val handlerJob = SupervisorJob(parentContext[Job])

    override val coroutineContext =
        parentContext +  // Parent context
        handlerJob +     // Supports failures of children
        exceptionHandler // Standard exception handler

}