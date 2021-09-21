plugins {
    kotlin("jvm") version "1.5.21"
    `maven-publish`
}

allprojects {
    group = "com.reposilite"
    version = "4.0.20"

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
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")
        implementation("io.javalin:javalin:4.0.0.RC3")

        val expressible = "1.0.15"
        api("org.panda-lang:expressible:$expressible")
        implementation("org.panda-lang:expressible-kt:$expressible")
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }
}