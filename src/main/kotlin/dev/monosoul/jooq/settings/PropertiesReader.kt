package dev.monosoul.jooq.settings

import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import org.gradle.api.Project
import kotlin.reflect.KMutableProperty0

internal object PropertiesReader {
    const val PREFIX = "dev.monosoul.jooq."
    const val WITH_CONTAINER = "${PREFIX}withContainer."
    const val WITHOUT_CONTAINER = "${PREFIX}withoutContainer."
    const val IMAGE_PREFIX = "image."
    const val DATABASE_PREFIX = "database."
    const val JDBC_PREFIX = "jdbc."

    fun Project.settingsFromProperties() = if (properties.keys.any { it.startsWith(WITHOUT_CONTAINER) }) {
        settingsWithoutContainerFromProperties()
    } else {
        settingsWithContainerFromProperties()
    }

    fun Project.settingsWithContainerFromProperties() = internalDatabaseFromProperties(
        jdbcFromProperties(WITH_CONTAINER)
    ).let {
        WithContainer(it, imageFromProperties(it))
    }

    fun Project.settingsWithoutContainerFromProperties() = WithoutContainer(
        externalDatabaseFromProperties(
            jdbcFromProperties(WITHOUT_CONTAINER)
        )
    )

    fun Project.jdbcFromProperties(namespace: String) = Jdbc().also { jdbc ->
        val prefix = "$namespace$JDBC_PREFIX"
        findAndSetProperty(prefix, jdbc::schema)
        findAndSetProperty(prefix, jdbc::driverClassName)
        findAndSetProperty(prefix, jdbc::urlQueryParams)
    }

    fun Project.externalDatabaseFromProperties(jdbc: Jdbc) = Database.External(jdbc = jdbc).also { db ->
        val prefix = "$WITHOUT_CONTAINER$DATABASE_PREFIX"
        findAndSetProperty(prefix, db::host)
        findAndSetProperty(prefix, db::port) { it.toInt() }
        findAndSetProperty(prefix, db::name)
        findAndSetProperty(prefix, db::username)
        findAndSetProperty(prefix, db::password)
    }

    fun Project.internalDatabaseFromProperties(jdbc: Jdbc) = Database.Internal(jdbc = jdbc).also { db ->
        val prefix = "$WITH_CONTAINER$DATABASE_PREFIX"
        findAndSetProperty(prefix, db::port) { it.toInt() }
        findAndSetProperty(prefix, db::name)
        findAndSetProperty(prefix, db::username)
        findAndSetProperty(prefix, db::password)
    }

    fun Project.imageFromProperties(database: Database.Internal) = Image(database).also { image ->
        val prefix = "$WITH_CONTAINER$IMAGE_PREFIX"
        findAndSetProperty(prefix, image::name)
        findAndSetProperty(prefix, image::command)
        findAndSetProperty(prefix, image::testQuery)

        val envVarsPrefix = "$prefix${image::envVars.name}."
        properties.filterKeys { it.startsWith(envVarsPrefix) }.map { (key, value) ->
            key.removePrefix(envVarsPrefix) to value.toString()
        }.toMap().takeIf { it.isNotEmpty() }?.also {
            image.envVars = it
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Project.findAndSetProperty(
        prefix: String,
        property: KMutableProperty0<T>,
        mapper: (String) -> T = { it as T }
    ) {
        (findProperty("$prefix${property.name}") as? String)?.also {
            property.set(mapper(it))
        }
    }
}
