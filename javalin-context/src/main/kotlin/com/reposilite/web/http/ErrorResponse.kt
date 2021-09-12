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
package com.reposilite.web.http

import io.javalin.http.HttpCode
import io.javalin.http.HttpCode.NOT_FOUND
import io.javalin.http.HttpCode.UNAUTHORIZED
import panda.std.Result
import panda.std.asError
import java.lang.System.lineSeparator

data class ErrorResponse(val status: Int, val message: String) {

    constructor(code: HttpCode, message: String) : this(code.status, message)

    fun updateMessage(transform: (String) -> String): ErrorResponse =
        ErrorResponse(status, transform(message))

}

fun <V> errorResponse(code: HttpCode, message: String): Result<V, ErrorResponse> =
    Result.error(ErrorResponse(code.status, message))

fun aggregatedError(code: HttpCode, errors: Collection<ErrorResponse>): ErrorResponse =
    ErrorResponse(
        code,
        "$code - Aggregated error" + lineSeparator() + errors.joinToString { lineSeparator() }
    )

private const val NOT_FOUND_MESSAGE = "Not found"

fun <V> notFoundError(message: String = NOT_FOUND_MESSAGE): Result<V, ErrorResponse> =
    notFound(message).asError()

fun notFound(message: String = NOT_FOUND_MESSAGE): ErrorResponse =
    ErrorResponse(NOT_FOUND, message)

private const val UNAUTHORIZED_MESSAGE = "Unauthorized access request"

fun <V> unauthorizedError(message: String = UNAUTHORIZED_MESSAGE): Result<V, ErrorResponse> =
    unauthorized(message).asError()

fun unauthorized(message: String = UNAUTHORIZED_MESSAGE): ErrorResponse =
    ErrorResponse(UNAUTHORIZED, message)
