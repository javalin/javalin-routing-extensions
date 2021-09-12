package com.reposilite.web

import io.javalin.core.util.JavalinLogger
import io.javalin.jetty.JettyUtil

inline fun runWithDisabledLogging(block: () -> Unit) {
    JavalinLogger.enabled = false
    JettyUtil.disableJettyLogger()
    block()
    JavalinLogger.enabled = true
    JettyUtil.reEnableJettyLogger()
}
