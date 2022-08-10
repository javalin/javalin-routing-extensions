/*
 * Copyright (c) 2021 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.web.routing

enum class RouteMethod {
    HEAD,
    PATCH,
    OPTIONS,
    GET,
    PUT,
    POST,
    DELETE,
    AFTER,
    BEFORE
}

interface Route {
    val path: String
    val methods: List<RouteMethod>
}

interface Routes<ROUTE : Route> {
    val routes: Set<ROUTE>
}

open class StandardRoute<CONTEXT, RESPONSE>(
    override val path: String,
    vararg methods: RouteMethod,
    open val handler: CONTEXT.() -> RESPONSE
) : Route {

    override val methods: List<RouteMethod> =
        methods.toList()

}

abstract class StandardRoutes<CONTEXT, RESPONSE : Any> : Routes<StandardRoute<CONTEXT, RESPONSE>> {

    protected fun route(path: String, vararg methods: RouteMethod, handler: CONTEXT.() -> RESPONSE): StandardRoute<CONTEXT, RESPONSE> =
        StandardRoute(
            path = path,
            methods = methods,
            handler = handler
        )

}
