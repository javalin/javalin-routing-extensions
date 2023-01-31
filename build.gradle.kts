import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    `maven-publish`
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    group = "io.javalin.community.routing"
    version = "5.3.2-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    publishing {
        repositories {
            maven {
                credentials {
                    username = property("mavenUser") as String
                    password = property("mavenPassword") as String
                }
                name = "panda-repository"
                url = when (version.toString().endsWith("-SNAPSHOT")) {
                    true -> uri("https://maven.reposilite.com/snapshots")
                    else -> uri("https://maven.reposilite.com/releases")
                }
            }
        }
        publications {
            create<MavenPublication>("library") {
                from(components.getByName("java"))
            }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
            languageVersion = "1.7"
            freeCompilerArgs = listOf(
                "-Xjvm-default=all", // For generating default methods in interfaces
                // "-Xcontext-receivers"
            )
        }
    }
}

subprojects {
    dependencies {
        val javalin = "5.3.2"
        compileOnly("io.javalin:javalin:$javalin")
        testImplementation("io.javalin:javalin:$javalin")

        val jackson = "2.14.0"
        testImplementation("com.fasterxml.jackson.core:jackson-databind:$jackson")
        testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson")

        val junit = "5.8.2"
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:$junit")
        testImplementation("org.assertj:assertj-core:3.24.1")

        val logback = "1.4.0"
        testImplementation("ch.qos.logback:logback-core:$logback")
        testImplementation("ch.qos.logback:logback-classic:$logback")
        testImplementation("org.slf4j:slf4j-api:2.0.0")
    }

    tasks.test {
        useJUnitPlatform()
    }
}