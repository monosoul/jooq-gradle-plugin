package dev.monosoul.jooq.settings

import java.io.Serializable

class Image(
    var name: String = "postgres:11.2-alpine",
    var envVars: MutableMap<String, String> = mutableMapOf(),
    var testQuery: String = "SELECT 1",
    var command: String? = null,
) : Serializable {
    constructor(database: Database.Internal) : this(
        envVars = mutableMapOf(
            "POSTGRES_USER" to database.username,
            "POSTGRES_PASSWORD" to database.password,
            "POSTGRES_DB" to database.name
        ),
    )
}
