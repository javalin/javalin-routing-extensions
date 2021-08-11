dependencies {
    api(project(":javalin-coroutines"))

    val logback = "1.2.5"
    testImplementation("ch.qos.logback:logback-core:$logback")
    testImplementation("ch.qos.logback:logback-classic:$logback")
    testImplementation("org.slf4j:slf4j-api:1.7.32")
}