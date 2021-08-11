package com.reposilite.web.http

import io.javalin.http.HttpCode

fun HttpCode.asString() =
    "$status: $message"