package dev.monosoul.jooq.migration

import dev.monosoul.jooq.JooqDockerPlugin.Companion.CONFIGURATION_NAME
import dev.monosoul.jooq.settings.DatabaseCredentials
import dev.monosoul.jooq.util.CodegenClasspathAwareClassLoaders
import org.flywaydb.core.api.Location.FILESYSTEM_PREFIX
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

    private val defaultFlywaySchema by lazy {
        flywayProperties.getting(DEFAULT_SCHEMA).orElse(schemas.map { it.first() }).get()
    }

    val flywayTableName by lazy {
        flywayProperties.getting(TABLE).getOrElse("flyway_schema_history")
    }

    fun migrateDb(classLoaders: CodegenClasspathAwareClassLoaders, credentials: DatabaseCredentials): SchemaVersion {
        return runCatching {
            ReflectiveMigrationRunner(classLoaders.buildscriptExclusive)
        }.onFailure {
            logger.debug("Failed to load Flyway from $CONFIGURATION_NAME classpath", it)
        }.onSuccess {
            logger.info("Loaded Flyway from $CONFIGURATION_NAME classpath")
        }.getOrElse {
            logger.info("Loaded Flyway from buildscript classpath")
            BuiltInMigrationRunner(classLoaders.buildscriptInclusive)
        }.migrateDb(
            schemas = schemas.get().toTypedArray(),
            migrationLocations = inputDirectory.map { "$FILESYSTEM_PREFIX${it.absolutePath}" }.toTypedArray(),
            flywayProperties = flywayProperties.get(),
            credentials = credentials,
            defaultFlywaySchema = defaultFlywaySchema,
            flywayTable = flywayTableName,
        )
    }
}
