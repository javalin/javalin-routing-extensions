package io.javalin.community.routing.annotations

import io.javalin.community.routing.dsl.DslRouteMetadata
import java.util.*

class AnnotatedEndpointMetadata(
    val annotations: Set<Annotation>
) : DslRouteMetadata {

    fun getAnnotation(annotationClass: Class<out Annotation>): Optional<Annotation> =
        annotations
            .firstOrNull { it.annotationClass.java == annotationClass }
            .let { Optional.ofNullable(it) }

}