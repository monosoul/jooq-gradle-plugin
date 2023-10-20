package dev.monosoul.jooq.artifact

import org.junit.jupiter.api.Test
import org.testcontainers.utility.MountableFile.forClasspathResource
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isSuccess

class WorksAfterFailingToFindDockerArtifactTest {

    @Test
    fun `should be possible to generate jooq classes even after it failed on first attempt`() {
        // given
        val gradleContainer = GradleContainer(dockerSocketPath = "/var/run/docker-alt.sock").apply {
            withEnv("TESTCONTAINERS_RYUK_DISABLED", "true")
            withCopyToContainer(forClasspathResource("/testproject"), projectPath)
            withCopyToContainer(forClasspathResource("/gradle_run.sh"), "/gradle_run.sh")
            withCommand("/gradle_run.sh")
        }

        // when & then
        expect {
            catching {
                gradleContainer.start()
                gradleContainer.stop()
            }.isSuccess()

            that(gradleContainer.output).contains("BUILD SUCCESSFUL in ")
        }
    }
}
