package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.java.exists
import strikt.java.notExists
import java.io.File

class IsolatedProjectsJooqDockerPluginFunctionalTest : FunctionalTestBase() {
    @Test
    fun `should work with isolated projects enabled`() {
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
        val result = runGradleWithArguments("generateJooqClasses", "--isolated-projects")

        // then
        expect {
            that(result).apply {
                generateJooqClassesTask.outcome isEqualTo SUCCESS
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists()
        }
    }
}
