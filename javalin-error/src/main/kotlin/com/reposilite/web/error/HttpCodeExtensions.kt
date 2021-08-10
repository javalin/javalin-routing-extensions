package com.reposilite.web.error

import io.javalin.http.HttpCode

fun HttpCode.asString() =
    "$status: $message"