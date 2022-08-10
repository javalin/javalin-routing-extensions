package com.reposilite.web.routing

import java.text.Collator
import java.text.RuleBasedCollator
import java.util.Locale

internal class RouteComparator : Comparator<Route> {

    private companion object {

        private val routesRule = RuleBasedCollator((Collator.getInstance(Locale.US) as RuleBasedCollator).rules.toString() + "& Z < '{' < '<'")

    }

    override fun compare(route: Route, other: Route): Int {
        val itPaths = route.path.split("/")
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

        return routesRule.compare(route.path, other.path)
    }

}