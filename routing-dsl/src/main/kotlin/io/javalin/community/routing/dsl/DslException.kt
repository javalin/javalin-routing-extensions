package io.javalin.community.routing.dsl

import kotlin.reflect.KClass

/* Exception handlers */

typealias DslExceptionHandler<CONTEXT, EXCEPTION, RESPONSE> = CONTEXT.(exception: EXCEPTION) -> RESPONSE

interface DslException<CONTEXT, EXCEPTION : Exception, RESPONSE : Any> {
    val type: KClass<EXCEPTION>
    val handler: DslExceptionHandler<CONTEXT, EXCEPTION, RESPONSE>
}

open class DefaultDslException<CONTEXT, EXCEPTION : Exception, RESPONSE : Any> (
    override val type: KClass<EXCEPTION>,
    override val handler: DslExceptionHandler<CONTEXT, EXCEPTION, RESPONSE>
) : DslException<CONTEXT, EXCEPTION, RESPONSE>