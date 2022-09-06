package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists

class ConfigurationCacheJooqDockerPluginFunctionalTest : FunctionalTestBase() {

    @Test
    fun `should work with configuration cache enabled`() {
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
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        val result = runGradleWithArguments("generateJooqClasses", "--configuration-cache")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
        }
    }
}
