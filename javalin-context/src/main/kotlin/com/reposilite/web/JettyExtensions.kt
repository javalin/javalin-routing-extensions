package com.reposilite.web.http

import org.eclipse.jetty.server.HttpOutput
import java.io.InputStream
import java.io.OutputStream

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