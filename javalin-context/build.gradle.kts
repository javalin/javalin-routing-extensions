plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))

    api(project(":javalin-error"))
    api(project(":javalin-mimetypes"))

    val javalin = "4.0.0.RC0"
    implementation("io.javalin:javalin:$javalin")
}