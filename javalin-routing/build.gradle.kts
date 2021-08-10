plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))

    val javalin = "4.0.0.RC0"
    implementation("io.javalin:javalin:$javalin")
}