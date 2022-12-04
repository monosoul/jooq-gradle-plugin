package dev.monosoul.jooq.smoke

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.testcontainers.containers.Network
import org.testcontainers.containers.Network.newNetwork
import strikt.api.expectThat
import strikt.assertions.contains

class PluginWorksSmokeTest {

    private lateinit var network: Network
    private val dbAlias = "postgresdb"
    private lateinit var postgresContainer: PostgresContainer

    @Before
    fun setUp() {
        network = newNetwork()
        postgresContainer = PostgresContainer()
            .withNetwork(network)
            .withNetworkAliases(dbAlias)
            .also { it.start() }
    }

    @After
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
        gradleContainer.start()

        // then
        val output = gradleContainer.output.joinToString("\n")
        expectThat(output).contains("BUILD SUCCESSFUL in ")
    }
}
