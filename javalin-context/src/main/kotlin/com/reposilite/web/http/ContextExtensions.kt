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

import io.javalin.http.ContentType
import io.javalin.http.Context
import org.eclipse.jetty.server.HttpOutput
import panda.std.Result
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

fun Context.acceptsBody(): Boolean =
    method() != "HEAD" && method() != "OPTIONS"

fun Context.error(error: ErrorResponse): Context =
    status(error.status).json(error)

fun Context.contentLength(length: Long): Context =
    also { res.setContentLengthLong(length) }

fun Context.encoding(encoding: Charset): Context =
    encoding(encoding.name())

fun Context.encoding(encoding: String): Context =
    also { res.characterEncoding = encoding }

fun Context.contentDisposition(disposition: String): Context =
    header("Content-Disposition", disposition)

object EmptyBody

data class HtmlResponse(val content: String)

fun Context.response(result: Any): Context =
    also {
        if (acceptsBody().not()) {
            return@also
        }

        when (result) {
            is Result<*, *> ->
                result.consume(
                    { value -> response(value) },
                    { error -> response(error) }
                )
            is ErrorResponse -> json(result).status(result.status)
            is HtmlResponse -> html(result.content)
            is InputStream -> result(result)
            is String -> result(result)
            is EmptyBody, Unit -> {}
            is Context -> {}
            else -> json(result)
        }
    }

fun Context.resultAttachment(name: String, contentType: ContentType, contentLength: Long, data: InputStream): Context {
    if (acceptsBody()) {
        data.transferLargeTo(res.outputStream)
    }
    else {
        data.close()
    }

    if (!contentType.isHumanReadable) {
        contentDisposition(""""attachment; filename="$name" """)
    }

    contentType(contentType)
    contentLength(contentLength)

    return this
}

fun InputStream.transferLargeTo(outputStream: OutputStream): Boolean =
    if (outputStream.isProbablyOpen()) {
        this.copyTo(outputStream)
        true
    }
    else false

fun OutputStream.isProbablyOpen(): Boolean =
    when (this) {
        is HttpOutput -> !isClosed
        else -> true
    }