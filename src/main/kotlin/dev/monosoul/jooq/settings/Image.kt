package dev.monosoul.jooq.settings

data class Image(
    var name: String = "postgres:14.4-alpine",
    var envVars: Map<String, String> = mapOf(),
    var testQuery: String = "SELECT 1",
    var command: String? = null,
) : SettingsElement {
    constructor(database: Database.Internal) : this(
        envVars = mapOf(
            "POSTGRES_USER" to database.username,
            "POSTGRES_PASSWORD" to database.password,
            "POSTGRES_DB" to database.name
        ),
    )
}
