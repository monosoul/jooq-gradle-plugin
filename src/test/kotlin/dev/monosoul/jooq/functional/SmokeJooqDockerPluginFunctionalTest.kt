package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class SmokeJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {
    @Test
    fun `plugin is applicable`() {
        // given
        prepareBuildGradleFile {
            """
            plugins {
                id("dev.monosoul.jooq-docker")
            }
            """.trimIndent()
        }

        // when
        val result = runGradleWithArguments("tasks")

        // then
        expectThat(result).getTaskOutcome("tasks") isEqualTo SUCCESS
    }
}
