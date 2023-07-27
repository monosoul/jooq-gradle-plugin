package dev.monosoul.jooq.migration

import dev.monosoul.jooq.JooqDockerPlugin.Companion.CONFIGURATION_NAME
import dev.monosoul.jooq.settings.DatabaseCredentials
import dev.monosoul.jooq.util.CodegenClasspathAwareClassLoaders
import org.flywaydb.core.internal.configuration.ConfigUtils.DEFAULT_SCHEMA
import org.flywaydb.core.internal.configuration.ConfigUtils.TABLE
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLClassLoader

internal class UniversalMigrationRunner(
    private val schemas: ListProperty<String>,
    private val migrationLocations: ListProperty<MigrationLocation>,
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
        val resolvedMigrationLocations = migrationLocations.get()
        logger.info("Migration locations: {}", resolvedMigrationLocations)

        val extraClasspath = resolvedMigrationLocations.flatMap { it.extraClasspath() }.also {
            logger.info("Migration will run using extra classpath: {}", it)
        }.toTypedArray()

        return runCatching {
            ReflectiveMigrationRunner(
                URLClassLoader(extraClasspath, classLoaders.buildscriptExclusive)
            )
        }.onFailure {
            logger.debug("Failed to load Flyway from $CONFIGURATION_NAME classpath", it)
        }.onSuccess {
            logger.info("Loaded Flyway from $CONFIGURATION_NAME classpath")
        }.getOrElse {
            logger.info("Loaded Flyway from buildscript classpath")
            BuiltInMigrationRunner(
                URLClassLoader(extraClasspath, classLoaders.buildscriptInclusive)
            )
        }.migrateDb(
            schemas = schemas.get().toTypedArray(),
            migrationLocations = resolvedMigrationLocations.flatMap { it.locations }.toTypedArray(),
            flywayProperties = flywayProperties.get(),
            credentials = credentials,
            defaultFlywaySchema = defaultFlywaySchema,
            flywayTable = flywayTableName,
        )
    }
}
