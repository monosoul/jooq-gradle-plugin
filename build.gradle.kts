import java.lang.Thread.sleep
import java.time.Duration

plugins {
    `kotlin-dsl`
    `kotlin-convention`
    `publishing-convention`
    `coverage-convention`
    `shadow-convention`
    `test-fixtures-convention`
    `linter-convention`
}

group = "dev.monosoul.jooq"

tasks {
    shadowJar {
        archiveClassifier.set("")
        // workaround to separate shadowed testcontainers configuration
        relocate("docker.client.strategy", "${project.group}.docker.client.strategy")
        relocate(
            "TESTCONTAINERS_DOCKER_CLIENT_STRATEGY",
            "${project.group.toString().uppercase().replace(".", "_")}_TESTCONTAINERS_DOCKER_CLIENT_STRATEGY",
        )
    }

    withType<Jar> {
        outputs.cacheIf { System.getenv("ENABLE_JAR_CACHING")?.lowercase() == "true" }
        if (System.getenv("ENABLE_JAR_CACHING")?.lowercase() == "true") {
            outputs.doNotCacheIfSpecs.clear()
        }
    }

    assemble {
        dependsOn(shadowJar)
    }

    pluginUnderTestMetadata {
        pluginClasspath.from(configurations.shadow) // provides complete plugin classpath to the Gradle testkit
    }

    processTemplates {
        filter {
            it
                .replace("@jooq.version@", libs.versions.jooq.get())
                .replace("@flyway.version@", libs.versions.flyway.get())
        }
    }
}

dependencies {
    /**
     * This is counter-intuitive, but dependencies in implementation or api configuration will actually
     * be shadowed, while dependencies in shadow configuration will be skipped from shadowing and just added as
     * transitive. This is a quirk of the shadow plugin.
     */
    shadow(libs.jooq.codegen)
    shadow(libs.bundles.flyway)
    shadow(platform(libs.jackson.bom))
    shadow("com.fasterxml.jackson.core:jackson-databind")
    shadow("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    shadow("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")

    implementation(libs.testcontainers.jdbc) {
        exclude(group = libs.jna.get().group) // cannot be shadowed
        exclude(group = "org.slf4j") // provided by Gradle
    }
    shadow(libs.jna)

    testFixturesApi(libs.testcontainers.postgresql)
    testFixturesApi(enforcedPlatform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter)
    testFixturesApi(libs.strikt)
    testFixturesApi(libs.mockk)
    testFixturesApi(gradleTestKit())
    testFixturesApi(libs.jna)
}

val functionalTestSuiteName = "functionalTest"
val extraTestSuiteName = "extraTest"

@Suppress("UnstableApiUsage")
testing {
    suites {
        register<JvmTestSuite>(functionalTestSuiteName) {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(testFixtures(project()))
                runtimeOnly(files(tasks.pluginUnderTestMetadata))
            }

            targets {
                all {
                    testTask.configure {
                        dependsOn(tasks.pluginUnderTestMetadata)
                        shouldRunAfter(tasks.test)
                        extensions.configure<JacocoTaskExtension> {
                            isEnabled = false
                        }

                        // workaround for https://github.com/gradle/gradle/issues/16603
                        doLast {
                            sleep(
                                Duration.ofSeconds(2).toMillis(),
                            )
                        }
                    }
                }
            }
        }

        register<JvmTestSuite>(extraTestSuiteName) {
            /**
             * This test suite is required because Gradle doesn't support Java agents
             * when using TestKit with configuration cache enabled
             *
             * https://docs.gradle.org/7.5.1/userguide/configuration_cache.html#config_cache:not_yet_implemented:testkit_build_with_java_agent
             */

            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(testFixtures(project()))
                runtimeOnly(files(tasks.pluginUnderTestMetadata))
            }

            targets {
                all {
                    testTask.configure {
                        dependsOn(tasks.pluginUnderTestMetadata)
                        shouldRunAfter(tasks.test)
                        extensions.configure<JacocoTaskExtension> {
                            isEnabled = false
                        }
                    }
                }
            }
        }
    }
}

jacocoTestKit {
    applyTo("${functionalTestSuiteName}RuntimeOnly", tasks.named(functionalTestSuiteName))
}

tasks.check {
    dependsOn(tasks.named(functionalTestSuiteName), tasks.named(extraTestSuiteName))
}
