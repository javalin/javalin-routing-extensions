package io.javalin.community.routing.dsl

import io.javalin.community.routing.Route
import io.javalin.community.routing.Routed
import io.javalin.http.Context
import io.javalin.router.EndpointMetadata

/* Regular routes */

typealias DslHandler<CONTEXT, RESPONSE> = CONTEXT.() -> RESPONSE

interface DslRouteMetadataFactory : EndpointMetadata {
    fun create(ctx: Context): DslRouteMetadata
}

interface DslRouteMetadata

interface DslRoute<CONTEXT, RESPONSE : Any> : Routed {
    val method: Route
    val version: String?
    val metadataFactory: DslRouteMetadataFactory?
    val handler: DslHandler<CONTEXT, RESPONSE>
}

open class DefaultDslRoute<CONTEXT, RESPONSE : Any>(
    override val method: Route,
    override val path: String,
    override val metadataFactory: DslRouteMetadataFactory? = null,
    override val version: String? = null,
    override val handler: CONTEXT.() -> RESPONSE
) : DslRoute<CONTEXT, RESPONSE>
