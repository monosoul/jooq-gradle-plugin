package dev.monosoul.jooq.migration

import dev.monosoul.jooq.settings.DatabaseCredentials
import org.flywaydb.core.Flyway

internal class BuiltInMigrationRunner(codegenAwareClassLoader: ClassLoader) : MigrationRunner {
    private val flyway = Flyway.configure(codegenAwareClassLoader)

    override fun migrateDb(
        schemas: Array<String>,
        migrationLocations: Array<String>,
        flywayProperties: Map<String, String>,
        credentials: DatabaseCredentials,
        defaultFlywaySchema: String,
        flywayTable: String,
    ) = flyway
        .dataSource(credentials.jdbcUrl, credentials.username, credentials.password)
        .schemas(*schemas)
        .locations(*migrationLocations)
        .defaultSchema(defaultFlywaySchema)
        .table(flywayTable)
        .configuration(flywayProperties)
        .load()
        .migrate()
        .targetSchemaVersion
        .let { SchemaVersion(it ?: "null") }
}
