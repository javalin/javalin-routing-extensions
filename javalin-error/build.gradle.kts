plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))

    val expressible = "1.0.4"
    api("org.panda-lang:expressible:$expressible")

    val javalin = "4.0.0.RC0"
    implementation("io.javalin:javalin:$javalin")
}