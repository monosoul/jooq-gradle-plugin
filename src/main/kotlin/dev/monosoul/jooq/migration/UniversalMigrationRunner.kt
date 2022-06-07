package dev.monosoul.jooq.migration

import dev.monosoul.jooq.JooqDockerPlugin
import dev.monosoul.jooq.settings.DatabaseCredentials
import dev.monosoul.jooq.util.CodegenClasspathAwareClassLoaders
import org.flywaydb.core.api.Location
import org.flywaydb.core.internal.configuration.ConfigUtils.DEFAULT_SCHEMA
import org.flywaydb.core.internal.configuration.ConfigUtils.TABLE
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class UniversalMigrationRunner(
    private val schemas: ListProperty<String>,
    private val inputDirectory: FileCollection,
    private val flywayProperties: MapProperty<String, String>,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun migrateDb(classLoaders: CodegenClasspathAwareClassLoaders, credentials: DatabaseCredentials): SchemaVersion {
        return runCatching {
            ReflectiveMigrationRunner(
                codegenAwareClassLoader = classLoaders.buildscriptExclusive,
                schemas = schemas.get().toTypedArray(),
                migrationLocations = inputDirectory.map { "${Location.FILESYSTEM_PREFIX}${it.absolutePath}" }
                    .toTypedArray(),
                flywayProperties = flywayProperties.get(),
                credentials = credentials,
                defaultFlywaySchema = defaultFlywaySchema(),
                flywayTable = flywayTableName(),
            )
        }.onFailure {
            logger.debug("Failed to load Flyway from ${JooqDockerPlugin.CONFIGURATION_NAME} classpath", it)
        }.onSuccess {
            logger.info("Loaded Flyway from ${JooqDockerPlugin.CONFIGURATION_NAME} classpath")
        }.getOrElse {
            logger.info("Loaded Flyway from buildscript classpath")
            BuiltInMigrationRunner(
                codegenAwareClassLoader = classLoaders.buildscriptInclusive,
                schemas = schemas.get().toTypedArray(),
                migrationLocations = inputDirectory.map { "${Location.FILESYSTEM_PREFIX}${it.absolutePath}" }
                    .toTypedArray(),
                flywayProperties = flywayProperties.get(),
                credentials = credentials,
                defaultFlywaySchema = defaultFlywaySchema(),
                flywayTable = flywayTableName(),
            )
        }.migrateDb()
    }

    fun defaultFlywaySchema() = flywayProperties.getting(DEFAULT_SCHEMA)
        .orElse(schemas.map { it.first() }).get()

    fun flywayTableName() = flywayProperties.getting(TABLE).getOrElse("flyway_schema_history")
}
