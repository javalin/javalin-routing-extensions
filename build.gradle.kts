plugins {
    kotlin("jvm") version "1.8.0"
    `maven-publish`
}

allprojects {
    group = "io.javalin.community.routing"
    version = "5.3.2-SNAPSHOT"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("io.javalin:javalin:5.3.2")

        val expressible = "1.3.0"
        api("org.panda-lang:expressible:$expressible")
        implementation("org.panda-lang:expressible-kt:$expressible")
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
}