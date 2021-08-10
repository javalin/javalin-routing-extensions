plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    api(project(":javalin-context"))

    val javalin = "4.0.0.RC0"
    implementation("io.javalin:javalin:$javalin")
}