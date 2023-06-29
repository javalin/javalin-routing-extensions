package io.javalin.community.routing

enum class Route(val isHttpMethod: Boolean = true) {
    HEAD,
    PATCH,
    OPTIONS,
    GET,
    PUT,
    POST,
    DELETE,
    AFTER(isHttpMethod = false),
    BEFORE(isHttpMethod = false),
}