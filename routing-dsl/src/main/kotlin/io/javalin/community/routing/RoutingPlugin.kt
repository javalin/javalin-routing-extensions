package io.javalin.community.routing

import io.javalin.Javalin
import io.javalin.community.routing.RouteMethod.AFTER
import io.javalin.community.routing.RouteMethod.BEFORE
import io.javalin.community.routing.RouteMethod.DELETE
import io.javalin.community.routing.RouteMethod.GET
import io.javalin.community.routing.RouteMethod.HEAD
import io.javalin.community.routing.RouteMethod.OPTIONS
import io.javalin.community.routing.RouteMethod.PATCH
import io.javalin.community.routing.RouteMethod.POST
import io.javalin.community.routing.RouteMethod.PUT
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit

class RoutingPlugin<ROUTE : Route>(
    private val handler: (Context, ROUTE) -> Unit
) : Plugin, PluginLifecycleInit {

    private val routing: MutableSet<ROUTE> = HashSet()

    override fun init(app: Javalin) { }

    override fun apply(app: Javalin) =
        routing
            .sortedWith(RouteComparator())
            .map { route -> Pair(route, createHandler(route)) }
            .forEach { (route, handler) ->
                route.methods.forEach { method ->
                    when (method) {
                        HEAD -> app.head(route.path, handler)
                        PATCH -> app.patch(route.path, handler)
                        OPTIONS -> app.options(route.path, handler)
                        GET -> app.get(route.path, handler)
                        PUT -> app.put(route.path, handler)
                        POST -> app.post(route.path, handler)
                        DELETE -> app.delete(route.path, handler)
                        AFTER -> app.after(route.path, handler)
                        BEFORE -> app.before(route.path, handler)
                    }
                }
            }

    private fun createHandler(route: ROUTE) =
        Handler { handler(it, route) }

    fun registerRoutes(routes: Set<ROUTE>): RoutingPlugin<ROUTE> =
        also { routing.addAll(routes) }

    fun registerRoutes(routes: Routes<ROUTE>): RoutingPlugin<ROUTE> =
        also { routing.addAll(routes.routes) }

}