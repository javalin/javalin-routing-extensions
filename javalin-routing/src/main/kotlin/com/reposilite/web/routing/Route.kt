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
    GET,
    PUT,
    POST,
    DELETE,
    AFTER,
    BEFORE
}

open class Route<CONTEXT>(
    val path: String,
    vararg val methods: RouteMethod,
    val async: Boolean = true,
    val handler: suspend CONTEXT.() -> Any
)


interface Routes<CONTEXT> {
    val routes: Set<Route<CONTEXT>>
}

abstract class AbstractRoutes<CONTEXT> : Routes<CONTEXT> {

    protected fun route(path: String, vararg methods: RouteMethod, async: Boolean = true, handler: suspend CONTEXT.() -> Any): Route<CONTEXT> =
        Route(path, methods = methods, async, handler)

}
