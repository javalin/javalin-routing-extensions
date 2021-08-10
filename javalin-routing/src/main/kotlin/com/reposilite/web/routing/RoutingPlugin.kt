package com.reposilite.web.routing

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.http.Context
import io.javalin.http.Handler

class RoutingPlugin<CONTEXT>(
    private val contextInitializer: (Context) -> CONTEXT
) : Plugin, PluginLifecycleInit {

    private val routing: MutableSet<Route<CONTEXT>> = HashSet()

    override fun init(app: Javalin) {
    }

    override fun apply(app: Javalin) {
        routing
            .sorted()
            .map { route ->
                Pair(route, Handler {
                    route.handler(contextInitializer(it))
                })
            }
            .forEach { (route, handler) ->
                route.methods.forEach { method ->
                    when (method) {
                        RouteMethod.HEAD -> app.head(route.path, handler)
                        RouteMethod.GET -> app.get(route.path, handler)
                        RouteMethod.PUT -> app.put(route.path, handler)
                        RouteMethod.POST -> app.post(route.path, handler)
                        RouteMethod.DELETE -> app.delete(route.path, handler)
                        RouteMethod.AFTER -> app.after(route.path, handler)
                        RouteMethod.BEFORE -> app.before(route.path, handler)
                    }
                }
            }
    }

    fun registerRoutes(vararg routes: Set<Route<CONTEXT>>) =
        routes.forEach {
            this.routing.addAll(it)
        }

    fun registerRoutes(vararg routes: Routes<CONTEXT>) =
        routes.forEach {
            this.routing.addAll(it.routes)
        }

}