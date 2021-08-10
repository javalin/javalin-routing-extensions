plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))

    val openapi = "1.1.0"
    implementation("io.javalin-rfc:javalin-openapi-plugin:$openapi")
}