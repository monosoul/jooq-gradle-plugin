package dev.monosoul.jooq.migration

import dev.monosoul.jooq.settings.DatabaseCredentials
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.internal.configuration.ConfigUtils.DEFAULT_SCHEMA
import org.flywaydb.core.internal.configuration.ConfigUtils.TABLE
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty

internal class MigrationRunner(
    private val schemas: ListProperty<String>,
    private val inputDirectory: ConfigurableFileCollection,
    private val flywayProperties: MapProperty<String, String>,
) {

    fun migrateDb(jdbcAwareClassLoader: ClassLoader, credentials: DatabaseCredentials): String = Flyway
        .configure(jdbcAwareClassLoader)
        .dataSource(credentials.jdbcUrl, credentials.username, credentials.password)
        .schemas(*schemas.get().toTypedArray())
        .locations(*inputDirectory.map { "${Location.FILESYSTEM_PREFIX}${it.absolutePath}" }.toTypedArray())
        .defaultSchema(defaultFlywaySchema())
        .table(flywayTableName())
        .configuration(flywayProperties.get())
        .load()
        .migrate()
        .targetSchemaVersion

    fun defaultFlywaySchema() = flywayProperties.getting(DEFAULT_SCHEMA)
        .orElse(schemas.map { it.first() }).get()

    fun flywayTableName() = flywayProperties.getting(TABLE).getOrElse("flyway_schema_history")
}
