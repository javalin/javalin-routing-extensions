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

import io.javalin.http.Handler
import java.text.Collator
import java.text.RuleBasedCollator
import java.util.Locale

open class Route<CONTEXT>(
    val path: String,
    vararg val methods: RouteMethod,
    val handler: CONTEXT.() -> Unit
) : Comparable<Route<*>> {

    private companion object {

        private val routesRule = RuleBasedCollator((Collator.getInstance(Locale.US) as RuleBasedCollator).rules.toString() + "& Z < '{' < '<'")

    }

    override fun compareTo(other: Route<*>): Int {
        val itPaths = path.split("/")
        val toPaths = other.path.split("/")
        var index = 0

        while (true) {
            if (index >= itPaths.size || index >= toPaths.size) {
                break
            }

            val itPart = itPaths[index]
            val toPart = toPaths[index]

            val result = routesRule.compare(itPart, toPart)

            if (result != 0) {
                return result
            }

            index++
        }

        return routesRule.compare(path, other.path)
    }

}

