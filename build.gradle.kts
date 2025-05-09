import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("kapt") version "1.9.22"
    jacoco
    signing
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

description = "Javalin Routing Extensions Parent | Parent project for Javalin Routing Extensions"

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    group = "io.javalin.community.routing"
    version = "6.6.0"

    repositories {
        mavenCentral()
        maven("https://maven.reposilite.com/snapshots")
    }

    if (version.toString().endsWith("-SNAPSHOT")) {
        publishing {
            repositories {
                maven {
                    credentials {
                        username = property("mavenUser") as String
                        password = property("mavenPassword") as String
                    }
                    name = "reposilite-repository"
                    url = uri("https://maven.reposilite.com/snapshots")
                }
            }
        }
    }

    afterEvaluate {
        description
            ?.takeIf { it.isNotEmpty() }
            ?.split("|")
            ?.let { (projectName, projectDescription) ->
                publishing {
                    publications {
                        create<MavenPublication>("library") {
                            pom {
                                name.set(projectName)
                                description.set(projectDescription)
                                url.set("https://github.com/javalin/javalin-routing-extensions")

                                licenses {
                                    license {
                                        name.set("The Apache License, Version 2.0")
                                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                    }
                                }
                                developers {
                                    developer {
                                        id.set("dzikoysk")
                                        name.set("dzikoysk")
                                        email.set("dzikoysk@dzikoysk.net")
                                    }
                                }
                                scm {
                                    connection.set("scm:git:git://github.com/javalin/javalin-routing-extensions.git")
                                    developerConnection.set("scm:git:ssh://github.com/javalin/javalin-routing-extensions.git")
                                    url.set("https://github.com/javalin/javalin-routing-extensions.git")
                                }
                            }

                            from(components.getByName("java"))
                        }
                    }
                }

                if (findProperty("signing.keyId").takeIf { it?.toString()?.trim()?.isNotEmpty() == true } != null) {
                    signing {
                        sign(publishing.publications.getByName("library"))
                    }
                }
            }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs = listOf(
            "-parameters"
        )
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
            languageVersion = "1.9"
            javaParameters = true
            freeCompilerArgs = listOf(
                "-Xjvm-default=all", // For generating default methods in interfaces
                // "-Xcontext-receivers" not yet :<
            )
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.kapt")

    dependencies {
        val javalin = "6.6.0"
        compileOnly("io.javalin:javalin:$javalin")
        testImplementation("io.javalin:javalin:$javalin")
        testImplementation("io.javalin:javalin-testtools:$javalin")

        val openapi = "6.6.0"
        kaptTest("io.javalin.community.openapi:openapi-annotation-processor:$openapi")
        testImplementation("io.javalin.community.openapi:javalin-openapi-plugin:$openapi")

        val jackson = "2.15.3"
        testImplementation("com.fasterxml.jackson.core:jackson-databind:$jackson")
        testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson")
        testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson")

        val unirest = "3.14.5"
        testImplementation("com.konghq:unirest-java:$unirest")

        val junit = "5.10.1"
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:$junit")
        testImplementation("org.assertj:assertj-core:3.24.2")

        val assertj = "3.24.2"
        testImplementation("org.assertj:assertj-core:$assertj")

        val logback = "1.4.11"
        testImplementation("ch.qos.logback:logback-core:$logback")
        testImplementation("ch.qos.logback:logback-classic:$logback")
        testImplementation("org.slf4j:slf4j-api:2.0.9")
    }

    tasks.test {
        useJUnitPlatform()
        finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test) // tests are required to run before generating the report

        reports {
            csv.required.set(false)
            xml.required.set(true)
            html.required.set(true)
        }

        finalizedBy("jacocoTestCoverageVerification")
    }

    tasks.jacocoTestCoverageVerification {
        violationRules {
            rule {
                limit {
                    minimum = "0.0".toBigDecimal()
                }
            }
            rule {
                enabled = true
                element = "CLASS"
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.0".toBigDecimal()
                }
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.0".toBigDecimal()
                }
                excludes = listOf()
            }
        }
    }
}

jacoco {
    toolVersion = "0.8.11"
}

nexusPublishing {
    repositories.sonatype {
        username.set(getEnvOrProperty("SONATYPE_USER", "sonatypeUser"))
        password.set(getEnvOrProperty("SONATYPE_PASSWORD", "sonatypePassword"))
    }
}

fun getEnvOrProperty(env: String, property: String): String? =
    System.getenv(env) ?: findProperty(property)?.toString()