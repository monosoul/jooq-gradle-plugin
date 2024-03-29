package dev.monosoul.jooq.artifact

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class PostgresContainer(
    image: String = "postgres:14.4-alpine",
) : PostgreSQLContainer<PostgresContainer>(DockerImageName.parse(image))
