package com.reposilite.web

import org.eclipse.jetty.server.HttpOutput
import java.io.Closeable
import java.io.OutputStream

fun OutputStream.isProbablyOpen(): Boolean =
    when (this) {
        is HttpOutput -> !isClosed
        else -> true
    }

fun Closeable.silentClose() =
    runCatching {
        this.close()
    }