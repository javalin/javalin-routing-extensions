rootProject.name = "javalin-routing-extensions"

include(
    "routing-core",
    "routing-annotations",
    "routing-annotations:routing-annotations-specification",
    // "routing-annotations:annotation-processor",
    "routing-annotations:routing-annotations",
    "routing-dsl",
    "routing-coroutines",
)
