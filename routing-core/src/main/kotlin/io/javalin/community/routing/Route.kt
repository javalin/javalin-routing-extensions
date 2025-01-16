package io.javalin.community.routing

enum class Route(
    val isHttpMethod: Boolean = true,
) {
    HEAD,
    PATCH,
    OPTIONS,
    GET,
    PUT,
    POST,
    DELETE,
    AFTER(isHttpMethod = false),
    AFTER_MATCHED(isHttpMethod = false),
    BEFORE(isHttpMethod = false),
    BEFORE_MATCHED(isHttpMethod = false),
}