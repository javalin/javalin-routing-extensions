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

package io.javalin.community.routing

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

interface Routed {
    val path: String
}

data class Route<CONTEXT, RESPONSE>(
    val method: RouteMethod,
    override val path: String,
    val handler: CONTEXT.() -> RESPONSE
) : Routed

fun interface Routes<CONTEXT, RESPONSE> {

    fun routes(): Collection<Route<CONTEXT, RESPONSE>>

    fun route(path: String, method: RouteMethod, handler: CONTEXT.() -> RESPONSE): Route<CONTEXT, RESPONSE> =
        Route(
            path = path,
            method = method,
            handler = handler
        )

}