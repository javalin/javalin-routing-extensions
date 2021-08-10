package com.reposilite.web.routing

interface Routes<CONTEXT> {
    val routes: Set<Route<CONTEXT>>
}