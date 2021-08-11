dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    api("org.panda-lang:expressible:1.0.4")

    val logback = "1.2.5"
    testImplementation("ch.qos.logback:logback-core:$logback")
    testImplementation("ch.qos.logback:logback-classic:$logback")
    testImplementation("org.slf4j:slf4j-api:1.7.32")
}