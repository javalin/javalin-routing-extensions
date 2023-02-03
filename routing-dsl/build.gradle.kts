description = "Routing DSL | Module provides set of DSLs for building routing in Javalin application"

dependencies {
    api(project(":routing-core"))
    implementation(kotlin("reflect"))
}