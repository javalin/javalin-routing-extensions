plugins {
    kotlin("jvm") version "1.5.20"
    `maven-publish`
}

allprojects {
    group = "com.reposilite"
    version = "1.0.6"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    publishing {
        repositories {
            maven {
                credentials {
                    username = property("mavenUser") as String
                    password = property("mavenPassword") as String
                }
                name = "panda-repository"
                url = uri("https://repo.panda-lang.org/releases")
            }
        }
        publications {
            create<MavenPublication>("library") {
                groupId = "$group.javalin-rfcs"
                from(components.getByName("java"))
            }
        }
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.panda-lang.org/releases") }
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation("io.javalin:javalin:4.0.0.RC0")
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }
}