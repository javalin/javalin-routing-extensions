plugins {
    kotlin("jvm") version "1.7.10"
    `maven-publish`
}

allprojects {
    group = "com.reposilite"
    version = "5.0.0-SNAPSHOT"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.panda-lang.org/releases") }
        maven { url = uri("https://repo.panda-lang.org/snapshots") }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
        implementation("io.javalin:javalin:5.0.0-SNAPSHOT")

        val expressible = "1.1.20"
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
                    true -> uri("https://repo.panda-lang.org/snapshots")
                    else -> uri("https://repo.panda-lang.org/releases")
                }
            }
        }
        publications {
            create<MavenPublication>("library") {
                groupId = "$group.javalin-rfcs"
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