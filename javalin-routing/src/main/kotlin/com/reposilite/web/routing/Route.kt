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

open class Route<CONTEXT, RESPONSE : Any>(
    val path: String,
    vararg val methods: RouteMethod,
    val async: Boolean = false,
    val handler: CONTEXT.() -> RESPONSE
)

interface Routes<CONTEXT, RESPONSE : Any> {
    val routes: Set<Route<CONTEXT, RESPONSE>>
}

abstract class AbstractRoutes<CONTEXT, RESPONSE : Any> : Routes<CONTEXT, RESPONSE> {

    protected fun route(path: String, vararg methods: RouteMethod, async: Boolean = false, handler: CONTEXT.() -> RESPONSE): Route<CONTEXT, RESPONSE> =
        Route(path, methods = methods, async, handler)

}
