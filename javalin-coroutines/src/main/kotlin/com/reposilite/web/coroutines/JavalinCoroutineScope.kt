package com.reposilite.web.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class JavalinCoroutineScope(
    parentContext: CoroutineContext,
    exceptionHandler: CoroutineExceptionHandler
) : CoroutineScope {

    private val handlerJob = SupervisorJob(parentContext[Job])

    override val coroutineContext =
        parentContext +  // Parent context
        handlerJob +     // Supports failures of children
        exceptionHandler // Standard exception handler

}