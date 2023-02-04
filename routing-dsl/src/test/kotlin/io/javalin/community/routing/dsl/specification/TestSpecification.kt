package io.javalin.community.routing.dsl.specification

import io.javalin.testtools.TestConfig

abstract class TestSpecification {

    protected val defaultConfig = TestConfig(captureLogs = false)

}