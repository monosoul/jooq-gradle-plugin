package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.java.exists

class JooqVersionConfigurationJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @Test
    fun `should provide recommended jOOQ version`() {
        // given
        prepareBuildGradleFile {
            """
                import dev.monosoul.jooq.RecommendedVersions
                
                plugins {
                    id("dev.monosoul.jooq-docker")
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    jooqCodegen("org.jooq:jooq-codegen:${'$'}{RecommendedVersions.JOOQ_VERSION}")
                    jooqCodegen("org.postgresql:postgresql:42.3.6")
                }
            """.trimIndent()
        }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.16.6", "3.15.10", "3.14.15"])
    fun `should be possible to specify jooq-codegen version to use`(jooqVersion: String) {
        // given
        prepareBuildGradleFile {
            """
                plugins {
                    id("dev.monosoul.jooq-docker")
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    jooqCodegen("org.jooq:jooq-codegen:$jooqVersion")
                    jooqCodegen("org.postgresql:postgresql:42.3.6")
                }
            """.trimIndent()
        }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result) {
                generateJooqClassesTask.outcome isEqualTo SUCCESS
                get { output } contains "Thank you for using jOOQ $jooqVersion"
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists().and {
                get { readText() } contains "jOOQ version:$jooqVersion"
            }
        }
    }
}
