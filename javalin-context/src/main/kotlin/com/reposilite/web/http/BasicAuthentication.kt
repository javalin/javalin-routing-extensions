package com.reposilite.web.http

import io.javalin.http.HttpCode.UNAUTHORIZED
import panda.std.Result
import panda.std.asSuccess
import java.util.Base64

const val AUTHORIZATION_HEADER = "Authorization"

fun extractFromHeaders(headers: Map<String, String>): Result<Pair<String, String>, ErrorResponse> =
    extractFromHeader(headers[AUTHORIZATION_HEADER])

fun extractFromHeader(authorizationHeader: String?): Result<Pair<String, String>, ErrorResponse> {
    if (authorizationHeader == null) {
        return errorResponse(UNAUTHORIZED, "Invalid authorization credentials")
    }

    val method = when {
        authorizationHeader.startsWith("Basic") -> "Basic" // Standard basic auth
        authorizationHeader.startsWith("xBasic") -> "xBasic" // Basic auth for browsers to avoid built-in auth popup
        else -> return errorResponse(UNAUTHORIZED, "Invalid authorization credentials")
    }

    return extractFromBase64(authorizationHeader.substring(method.length).trim())
}

fun extractFromBase64(basicCredentials: String): Result<Pair<String, String>, ErrorResponse> =
    extractFromString(Base64.getDecoder().decode(basicCredentials).decodeToString())

fun extractFromString(credentials: String): Result<Pair<String, String>, ErrorResponse> =
    credentials
        .split(":", limit = 2)
        .takeIf { it.size == 2 }
        ?.let { (username, password) -> Pair(username, password).asSuccess() }
        ?: errorResponse(UNAUTHORIZED, "Invalid authorization credentials")