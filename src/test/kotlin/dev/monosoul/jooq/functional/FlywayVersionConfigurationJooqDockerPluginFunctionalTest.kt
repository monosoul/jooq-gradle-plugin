package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.java.exists

class FlywayVersionConfigurationJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @Test
    fun `should provide recommended Flyway version`() {
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
                    jooqCodegen("org.flywaydb:flyway-core:${'$'}{RecommendedVersions.FLYWAY_VERSION}")
                    jooqCodegen("org.flywaydb:flyway-database-postgresql:${'$'}{RecommendedVersions.FLYWAY_VERSION}")
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
    @ValueSource(strings = ["9.22.3", "8.5.12", "8.4.4", "8.0.5", "7.15.0"])
    fun `should be possible to specify Flyway version to use`(flywayVersion: String) {
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
                    jooqCodegen("org.flywaydb:flyway-core:$flywayVersion")
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
                get { output } contains "Flyway Community Edition $flywayVersion by Redgate"
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
        }
    }
}
