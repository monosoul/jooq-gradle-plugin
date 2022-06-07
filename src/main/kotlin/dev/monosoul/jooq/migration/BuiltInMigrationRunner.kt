package dev.monosoul.jooq.migration

import dev.monosoul.jooq.settings.DatabaseCredentials
import org.flywaydb.core.Flyway

internal class BuiltInMigrationRunner(
    codegenAwareClassLoader: ClassLoader,
    private val schemas: Array<String>,
    private val migrationLocations: Array<String>,
    private val flywayProperties: Map<String, String>,
    private val credentials: DatabaseCredentials,
    private val defaultFlywaySchema: String,
    private val flywayTable: String,
) : MigrationRunner {

    private val flyway = Flyway.configure(codegenAwareClassLoader)

    override fun migrateDb() = flyway
        .dataSource(credentials.jdbcUrl, credentials.username, credentials.password)
        .schemas(*schemas)
        .locations(*migrationLocations)
        .defaultSchema(defaultFlywaySchema)
        .table(flywayTable)
        .configuration(flywayProperties)
        .load()
        .migrate()
        .targetSchemaVersion
        .let(::SchemaVersion)
}
