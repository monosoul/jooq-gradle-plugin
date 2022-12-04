package dev.monosoul.jooq.artifact

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.Network
import org.testcontainers.containers.Network.newNetwork
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isSuccess

class PluginWorksArtifactTest {

    private lateinit var network: Network
    private val dbAlias = "postgresdb"
    private lateinit var postgresContainer: PostgresContainer

    @BeforeEach
    fun setUp() {
        network = newNetwork()
        postgresContainer = PostgresContainer()
            .withNetwork(network)
            .withNetworkAliases(dbAlias)
            .also { it.start() }
    }

    @AfterEach
    fun tearDown() {
        postgresContainer.stop()
        network.close()
    }

    /**
     * Runs the plugin in a containerized environment to make sure the artifact produced actually works
     */
    @Test
    fun `should be possible to load the plugin and generate jooq classes`() {
        // given
        val gradleContainer = GradleContainer().withNetwork(network).withCommand(
            "gradle",
            "classes",
            "-Pdev.monosoul.jooq.withoutContainer.db.username=${postgresContainer.username}",
            "-Pdev.monosoul.jooq.withoutContainer.db.password=${postgresContainer.password}",
            "-Pdev.monosoul.jooq.withoutContainer.db.name=${postgresContainer.databaseName}",
            "-Pdev.monosoul.jooq.withoutContainer.db.host=$dbAlias",
            "-Pdev.monosoul.jooq.withoutContainer.db.port=${postgresContainer.exposedPorts.first()}",
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
