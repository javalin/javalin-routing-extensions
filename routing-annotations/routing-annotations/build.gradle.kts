description = "Routing Annotations | Module provides implementation of annotation based routing"

dependencies {
    api(project(":routing-dsl"))
    api(project(":routing-annotations:routing-annotations-specification"))

    implementation(kotlin("reflect"))
}