description = "Routing Annotated | Module provides implementation of annotation based routing"

dependencies {
    api(project(":routing-dsl"))
    api(project(":routing-annotations:routing-annotated-specification"))

    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:2.2.20")
}