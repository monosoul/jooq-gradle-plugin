package dev.monosoul.jooq.artifact

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isSuccess

class PluginWorksArtifactTest {

    /**
     * Runs the plugin in a containerized environment to make sure the artifact produced actually works
     */
    @Test
    fun `should be possible to load the plugin and generate jooq classes`() {
        // given
        val gradleContainer = GradleContainer().withCommand(
            "gradle",
            "classes",
            "--info",
        )

        // when
        expectCatching {
            gradleContainer.start()
            gradleContainer.stop()
        }.isSuccess()

        // then
        val output = gradleContainer.output.joinToString("\n")
        expectThat(output).contains("BUILD SUCCESSFUL in ")
    }
}
