package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo

class NoMigrationsFunctionalTest : JooqDockerPluginFunctionalTestBase() {
    @Test
    fun `should not cause an exception when there are no migrations with built-in Flyway`() {
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
                jooqCodegen("org.postgresql:postgresql:42.3.6")
            }
            """.trimIndent()
        }

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
        }
    }

    @Test
    fun `should not cause an exception when there are no migrations with provided Flyway`() {
        // given
        prepareBuildGradleFile {
            """
            import dev.monosoul.jooq.RecommendedVersions.FLYWAY_VERSION
                            
            plugins {
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                jooqCodegen("org.flywaydb:flyway-core:${'$'}FLYWAY_VERSION")
                jooqCodegen("org.flywaydb:flyway-database-postgresql:${'$'}FLYWAY_VERSION")
                jooqCodegen("org.postgresql:postgresql:42.3.6")
            }
            """.trimIndent()
        }

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
        }
    }
}
