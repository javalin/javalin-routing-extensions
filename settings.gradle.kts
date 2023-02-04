rootProject.name = "javalin-routing-extensions"

include(
    "routing-core",
    "routing-annotations",
    "routing-annotations:routing-annotated-specification",
    // "routing-annotations:annotation-processor",
    "routing-annotations:routing-annotated",
    "routing-dsl",
    "routing-coroutines",
)
