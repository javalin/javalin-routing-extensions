# Setup

Javalin Routing Extensions is a set of modular plugins that extend Javalin's routing system with popular patterns like annotations, DSL, and coroutines. Each module is distributed as a separate artifact -- install only what you need.

## Requirements

- Java 17+
- Javalin 7.x

## Installation

::: code-group

```kotlin [Gradle (Kotlin)]
repositories {
    mavenCentral()
}

dependencies {
    val routingExtensions = "7.0.1"

    // Annotated routing (Java & Kotlin)
    implementation(
        "io.javalin.community.routing:routing-annotated:$routingExtensions"
    )
    // DSL routing (Kotlin)
    implementation(
        "io.javalin.community.routing:routing-dsl:$routingExtensions"
    )
    // Coroutines routing (Kotlin)
    implementation(
        "io.javalin.community.routing:routing-coroutines:$routingExtensions"
    )
}
```

```groovy [Gradle (Groovy)]
repositories {
    mavenCentral()
}

dependencies {
    def routingExtensions = "7.0.1"

    implementation "io.javalin.community.routing:routing-annotated:$routingExtensions"
    implementation "io.javalin.community.routing:routing-dsl:$routingExtensions"
    implementation "io.javalin.community.routing:routing-coroutines:$routingExtensions"
}
```

```xml [Maven]
<dependencies>
    <!-- Annotated routing (Java & Kotlin) -->
    <dependency>
        <groupId>io.javalin.community.routing</groupId>
        <artifactId>routing-annotated</artifactId>
        <version>7.0.1</version>
    </dependency>
    <!-- DSL routing (Kotlin) -->
    <dependency>
        <groupId>io.javalin.community.routing</groupId>
        <artifactId>routing-dsl</artifactId>
        <version>7.0.1</version>
    </dependency>
    <!-- Coroutines routing (Kotlin) -->
    <dependency>
        <groupId>io.javalin.community.routing</groupId>
        <artifactId>routing-coroutines</artifactId>
        <version>7.0.1</version>
    </dependency>
</dependencies>
```

:::

You only need to include the modules you plan to use. All modules depend on `routing-core` transitively, so you don't need to add it explicitly if you're using any of the routing modules.

## Compiler Configuration

If you use **annotated routing** with named parameters (e.g., `@Param String name` without specifying the parameter name explicitly), you must pass the `-parameters` flag to your Java compiler to preserve parameter names in bytecode:

::: code-group

```kotlin [Gradle (Kotlin)]
tasks.withType<JavaCompile> {
    options.compilerArgs = listOf("-parameters")
}
```

```groovy [Gradle (Groovy)]
tasks.withType(JavaCompile) {
    options.compilerArgs = ['-parameters']
}
```

```xml [Maven]
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <compilerArgs>
                    <arg>-parameters</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

:::

For Kotlin, enable `javaParameters` in the compiler options:

```kotlin
kotlin {
    compilerOptions {
        javaParameters.set(true)
    }
}
```

## Module Compatibility

| Module | Languages | Reflection | Use Case |
|--------|-----------|------------|----------|
| [Annotated](../annotated/getting-started) | Java, Kotlin | Yes | Annotation-driven routing |
| [DSL In-place](../dsl/in-place) | Kotlin | Optional | Inline route definitions |
| [DSL Properties](../dsl/property-based) | Kotlin | Optional | Route containers with property-based definitions |
| [Coroutines](../coroutines/getting-started) | Kotlin | No | Async/non-blocking endpoint execution |

## Next Steps

- [Overview](./overview) — understand the architecture and choose your approach
- [Annotated Routing](../annotated/getting-started) — get started with annotation-driven routing
- [DSL Routing](../dsl/getting-started) — get started with Kotlin DSL routing
- [Coroutines Routing](../coroutines/getting-started) — get started with async routing
