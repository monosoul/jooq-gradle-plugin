package dev.monosoul.jooq.artifact

import org.junit.jupiter.api.Test
import org.testcontainers.utility.MountableFile.forClasspathResource
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isSuccess

class PluginWorksArtifactTest {

    /**
     * Runs the plugin in a containerized environment to make sure the artifact produced actually works
     */
    @Test
    fun `should be possible to load the plugin and generate jooq classes`() {
        // given
        val gradleContainer = GradleContainer().apply {
            withCopyToContainer(forClasspathResource("/testproject"), projectPath)
            withCopyToContainer(
                forClasspathResource("/.testcontainers.properties.template"),
                "/root/.testcontainers.properties"
            )
            withEnv("TESTCONTAINERS_DOCKER_CLIENT_STRATEGY", "org.testcontainers.dockerclient.UnixSocketClientProviderStrategy")
            withCommand(
                "gradle",
                "classes",
                "--info",
                "--stacktrace",
            )
        }

        // when & then
        expect {
            catching {
                gradleContainer.start()
                gradleContainer.stop()
            }.isSuccess()

            val output = gradleContainer.output.joinToString("\n")
            that(output).contains("BUILD SUCCESSFUL in ")
        }
    }
}
