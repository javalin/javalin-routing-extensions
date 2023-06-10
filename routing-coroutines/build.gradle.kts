description = "Routing Coroutines | Module provides coroutines support for Routing DSL"

dependencies {
    api(project(":routing-dsl"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    val logback = "1.4.5"
    testImplementation("ch.qos.logback:logback-core:$logback")
    testImplementation("ch.qos.logback:logback-classic:$logback")
    testImplementation("org.slf4j:slf4j-api:2.0.7")
}