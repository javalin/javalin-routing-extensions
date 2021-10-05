dependencies {
    api(project(":javalin-context"))
    api(project(":javalin-routing"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("org.eclipse.jetty:jetty-server:9.4.43.v20210629")

    val logback = "1.2.5"
    testImplementation("ch.qos.logback:logback-core:$logback")
    testImplementation("ch.qos.logback:logback-classic:$logback")
    testImplementation("org.slf4j:slf4j-api:1.7.32")
}