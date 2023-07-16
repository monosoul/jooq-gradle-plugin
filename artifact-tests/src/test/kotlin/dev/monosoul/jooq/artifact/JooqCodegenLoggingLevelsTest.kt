package dev.monosoul.jooq.artifact

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.MountableFile.forClasspathResource
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isSuccess

class JooqCodegenLoggingLevelsTest {

    @ParameterizedTest
    @ValueSource(strings = ["", "--quiet"])
    fun `should not have jOOQ codegen warnings with non verbose log levels`(logLevel: String) {
        // given
        val gradleContainer = GradleContainer().apply {
            setUp()
            val arguments = listOfNotNull(
                "gradle",
                "generateJooqClasses",
                logLevel.takeIf { it.isNotBlank() }
            ).toTypedArray()
            withCommand(*arguments)
        }

        // when & then
        expect {
            catching {
                gradleContainer.start()
                gradleContainer.stop()
            }.isSuccess()

            val output = gradleContainer.output.joinToString("\n")
            that(output).apply {
                not().contains("Database version is older than what dialect POSTGRES supports")
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["--info", "--warn", "--debug"])
    fun `should have jOOQ codegen warnings with verbose log levels`(logLevel: String) {
        // given
        val gradleContainer = GradleContainer().apply {
            setUp()
            withCommand("gradle", "generateJooqClasses", logLevel)
        }

        // when & then
        expect {
            catching {
                gradleContainer.start()
                gradleContainer.stop()
            }.isSuccess()

            val output = gradleContainer.output.joinToString("\n")
            that(output).apply {
                contains("Database version is older than what dialect POSTGRES supports")
            }
        }
    }

    private fun GradleContainer.setUp() {
        withCopyToContainer(forClasspathResource("/testproject/src"), "$projectPath/src")
        withCopyToContainer(
            Transferable.of(
                """
                    import org.jooq.meta.jaxb.Logging

                    plugins {
                        id("dev.monosoul.jooq-docker") version "${Versions.PLUGIN_VERSION}"
                    }

                    repositories {
                        mavenCentral()
                    }

                    tasks {
                        generateJooqClasses {
                            withContainer {
                                image {
                                    name = "postgres:14.4-alpine"
                                }
                            }
                        }
                    }

                    dependencies {
                        jooqCodegen("org.postgresql:postgresql:42.3.6")
                    }
                """.trimIndent()
            ),
            "$projectPath/build.gradle.kts"
        )
        withCopyToContainer(
            Transferable.of(
                """
                    pluginManagement {
                        repositories {
                            maven {
                                name = "localBuild"
                                url = uri("./local-repository")
                            }
                            mavenCentral()
                            gradlePluginPortal {
                                content {
                                    excludeGroup("org.jooq")
                                    excludeGroup("org.flywaydb")
                                    excludeGroupByRegex("com\\.fasterxml.*")
                                    excludeGroupByRegex("com\\.google.*")
                                    excludeGroupByRegex("org\\.junit.*")
                                    excludeGroupByRegex("net\\.java.*")
                                    excludeGroupByRegex("jakarta.*")
                                }
                            }
                        }
                    }
                """.trimIndent()
            ),
            "$projectPath/settings.gradle.kts"
        )
    }
}
