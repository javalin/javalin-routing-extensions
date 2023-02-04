description = "Routing Annotated | Module provides implementation of annotation based routing"

dependencies {
    api(project(":routing-dsl"))
    api(project(":routing-annotations:routing-annotated-specification"))

    implementation(kotlin("reflect"))
}