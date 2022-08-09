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

import com.reposilite.web.isProbablyOpen
import com.reposilite.web.silentClose
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.HttpStatus
import panda.std.Result
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

object EmptyBody

data class HtmlResponse(val content: String)

fun Context.response(result: Any): Context =
    also {
        when (result) {
            is EmptyBody, Unit -> return@also
            is Context -> return@also
            is Result<*, *> -> {
                result.consume(
                    { value -> response(value) },
                    { error -> response(error) }
                )
                return@also
            }
        }

        if (!acceptsBody() || !output().isProbablyOpen()) {
            if (result is InputStream) {
                result.silentClose()
            }

            return@also
        }

        clearContentLength()

        when (result) {
            is ErrorResponse -> error(result)
            is HtmlResponse -> html(result.content)
            is InputStream -> result(result)
            is String -> result(result)
            else -> json(result)
        }
    }

fun Context.acceptsBody(): Boolean =
    method() != HEAD && method() != OPTIONS

fun Context.clearContentLength(): Context =
    also { contentLength(-1) }

fun Context.contentLength(length: Long): Context =
    also { res().setContentLengthLong(length) }

fun Context.encoding(encoding: Charset): Context =
    encoding(encoding.name())

fun Context.encoding(encoding: String): Context =
    also { res().characterEncoding = encoding }

fun Context.contentDisposition(disposition: String): Context =
    header("Content-Disposition", disposition)

fun Context.resultAttachment(name: String, contentType: ContentType, contentLength: Long, data: InputStream): Context {
    contentType(contentType)

    if (contentLength > 0) {
        contentLength(contentLength)
    }

    if (!contentType.isHumanReadable) {
        contentDisposition(""""attachment; filename="$name" """)
    }

    return response(data)
}

fun Context.uri(): String =
    path()

fun Context.output(): OutputStream =
    res().outputStream

fun Context.error(error: ErrorResponse): Context =
    error(error.status, error)

fun Context.error(status: HttpStatus, error: Any): Context =
    error(status.code, error)

fun Context.error(status: Int, error: Any): Context =
    status(status).json(error)