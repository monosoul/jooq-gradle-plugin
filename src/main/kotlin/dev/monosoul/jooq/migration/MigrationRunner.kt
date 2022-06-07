package dev.monosoul.jooq.migration

import dev.monosoul.jooq.settings.DatabaseCredentials

internal interface MigrationRunner {
    fun migrateDb(
        schemas: Array<String>,
        migrationLocations: Array<String>,
        flywayProperties: Map<String, String>,
        credentials: DatabaseCredentials,
        defaultFlywaySchema: String,
        flywayTable: String,
    ): SchemaVersion
}
