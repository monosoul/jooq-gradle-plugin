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

    assemble {
        dependsOn(shadowJar)
    }

    pluginUnderTestMetadata {
        pluginClasspath.from(configurations.shadow) // provides complete plugin classpath to the Gradle testkit
    }

    processTemplates {
        filter {
            it.replace("@jooq.version@", libs.versions.jooq.get())
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
}

val functionalTestSuiteName = "functionalTest"

testing {
    @Suppress("UnstableApiUsage")
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
    }
}

jacocoTestKit {
    applyTo("${functionalTestSuiteName}RuntimeOnly", tasks.named(functionalTestSuiteName))
}

tasks.check {
    dependsOn(tasks.named(functionalTestSuiteName))
}
