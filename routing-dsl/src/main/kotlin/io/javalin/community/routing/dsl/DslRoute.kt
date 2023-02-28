package io.javalin.community.routing.dsl

import io.javalin.community.routing.Route
import io.javalin.community.routing.Routed

/* Regular routes */

typealias DslHandler<CONTEXT, RESPONSE> = CONTEXT.() -> RESPONSE

interface DslRoute<CONTEXT, RESPONSE : Any> : Routed {
    val method: Route
    val version: String?
    val handler: DslHandler<CONTEXT, RESPONSE>
}

open class DefaultDslRoute<CONTEXT, RESPONSE : Any>(
    override val method: Route,
    override val path: String,
    override val version: String? = null,
    override val handler: CONTEXT.() -> RESPONSE
) : DslRoute<CONTEXT, RESPONSE>
