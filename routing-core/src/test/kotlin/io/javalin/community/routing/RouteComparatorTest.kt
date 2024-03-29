package io.javalin.community.routing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RouteComparatorTest {

    private data class Route(
        override val path: String
    ) : Routed

    @Test
    fun `should sort regular paths in descending order`() {
        // given: a list of routes
        val routes = listOf(
            "/base",
            "/base/details",
            "/"
        )

        // when: the routes are sorted
        val sortedRoutes = routes
            .map(::Route)
            .sortRoutes()
            .map { it.path }

        // then: the routes are sorted by path length
        assertThat(sortedRoutes).containsExactly(
            "/",
            "/base/details",
            "/base",
        )
    }

    @Test
    fun `should prioritize concrete paths over wildcard paths`() {
        // given: a list of routes
        val routes = listOf(
            "/<all>",
            "/<all>/custom/<sub>",
            "/base/details",
            "/base/{id}",
            "/"
        )

        // when: the routes are sorted
        val sortedRoutes = routes
            .map(::Route)
            .sortRoutes()
            .map { it.path }

        // then: the routes are sorted by path length
        assertThat(sortedRoutes).containsExactly(
            "/",
            "/base/details",
            "/base/{id}",
            "/<all>/custom/<sub>",
            "/<all>"
        )
    }

}