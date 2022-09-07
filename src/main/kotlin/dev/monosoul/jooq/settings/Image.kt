package dev.monosoul.jooq.settings

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

data class Image(
    @get:Input var name: String = "postgres:14.4-alpine",
    @get:Input var envVars: Map<String, String> = mapOf(),
    @get:Input var testQuery: String = "SELECT 1",
    @get:Input @get:Optional var command: String? = null,
) : SettingsElement {
    constructor(database: Database.Internal) : this(
        envVars = mapOf(
            "POSTGRES_USER" to database.username,
            "POSTGRES_PASSWORD" to database.password,
            "POSTGRES_DB" to database.name
        ),
    )
}
