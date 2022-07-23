package com.reposilite.web.routing

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit

class RoutingPlugin<CONTEXT, RESPONSE : Any>(
    private val handler: (Context, Route<CONTEXT, RESPONSE>) -> RESPONSE
) : Plugin, PluginLifecycleInit {

    private val routing: MutableSet<Route<CONTEXT, RESPONSE>> = HashSet()

    override fun init(app: Javalin) { }

    override fun apply(app: Javalin) =
        routing
            .sortedWith(RouteComparator())
            .map { route -> Pair(route, createHandler(route)) }
            .forEach { (route, handler) ->
                route.methods.forEach { method ->
                    when (method) {
                        RouteMethod.HEAD -> app.head(route.path, handler)
                        RouteMethod.PATCH -> app.patch(route.path, handler)
                        RouteMethod.OPTIONS -> app.options(route.path, handler)
                        RouteMethod.GET -> app.get(route.path, handler)
                        RouteMethod.PUT -> app.put(route.path, handler)
                        RouteMethod.POST -> app.post(route.path, handler)
                        RouteMethod.DELETE -> app.delete(route.path, handler)
                        RouteMethod.AFTER -> app.after(route.path, handler)
                        RouteMethod.BEFORE -> app.before(route.path, handler)
                    }
                }
            }

    private fun createHandler(route: Route<CONTEXT, RESPONSE>) =
        Handler { handler(it, route) }

    fun registerRoutes(routes: Set<Route<CONTEXT, RESPONSE>>): RoutingPlugin<CONTEXT, RESPONSE> =
        also { routing.addAll(routes) }

    fun registerRoutes(routes: Routes<CONTEXT, RESPONSE>): RoutingPlugin<CONTEXT, RESPONSE> =
        also { routing.addAll(routes.routes) }

}