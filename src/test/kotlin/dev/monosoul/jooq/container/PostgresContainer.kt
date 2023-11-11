package dev.monosoul.jooq.container

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

internal class PostgresContainer(
    image: String = "postgres:14.4-alpine",
) : PostgreSQLContainer<PostgresContainer>(DockerImageName.parse(image))
