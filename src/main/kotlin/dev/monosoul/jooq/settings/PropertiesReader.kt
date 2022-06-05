package dev.monosoul.jooq.settings

import dev.monosoul.jooq.SettingsAware
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import org.gradle.api.Action
import org.gradle.api.Project
import kotlin.reflect.KFunction2
import kotlin.reflect.KMutableProperty0

internal object PropertiesReader {
    const val PREFIX = "dev.monosoul.jooq."
    val WITH_CONTAINER = "${PREFIX}${functionName(SettingsAware::withContainer)}."
    val WITHOUT_CONTAINER = "${PREFIX}${functionName(SettingsAware::withoutContainer)}."
    val IMAGE_PREFIX = "${functionName(WithContainer::image)}."
    val DATABASE_PREFIX = "${functionName(DbAware<Database>::db)}."
    val JDBC_PREFIX = "${functionName(Database::jdbc)}."

    fun WithContainer.applyPropertiesFrom(project: Project): JooqDockerPluginSettings =
        if (project.properties.keys.any { it.startsWith(WITHOUT_CONTAINER) }) {
            WithoutContainer {
                applyPropertiesFrom(project)
            }
        } else {
            onlyApplyPropertiesFrom(project)
        }

    private fun WithContainer.onlyApplyPropertiesFrom(project: Project) = apply {
        image.applyPropertiesFrom(project)
        database.applyPropertiesFrom(project)
    }

    fun WithoutContainer.applyPropertiesFrom(project: Project): JooqDockerPluginSettings = apply {
        database.applyPropertiesFrom(project)
    }

    fun Jdbc.applyPropertiesFrom(project: Project, namespace: String) {
        val prefix = "$namespace$JDBC_PREFIX"
        project.findAndSetProperty(prefix, ::schema)
        project.findAndSetProperty(prefix, ::driverClassName)
        project.findAndSetProperty(prefix, ::urlQueryParams)
    }

    fun Database.External.applyPropertiesFrom(project: Project) {
        val prefix = "$WITHOUT_CONTAINER$DATABASE_PREFIX"
        project.findAndSetProperty(prefix, ::host)
        project.findAndSetProperty(prefix, ::port) { it.toInt() }
        project.findAndSetProperty(prefix, ::name)
        project.findAndSetProperty(prefix, ::username)
        project.findAndSetProperty(prefix, ::password)
        jdbc.applyPropertiesFrom(project, prefix)
    }

    fun Database.Internal.applyPropertiesFrom(project: Project) {
        val prefix = "$WITH_CONTAINER$DATABASE_PREFIX"
        project.findAndSetProperty(prefix, ::port) { it.toInt() }
        project.findAndSetProperty(prefix, ::name)
        project.findAndSetProperty(prefix, ::username)
        project.findAndSetProperty(prefix, ::password)
        jdbc.applyPropertiesFrom(project, prefix)
    }

    fun Image.applyPropertiesFrom(project: Project) {
        val prefix = "$WITH_CONTAINER$IMAGE_PREFIX"
        project.findAndSetProperty(prefix, ::name)
        project.findAndSetProperty(prefix, ::command)
        project.findAndSetProperty(prefix, ::testQuery)

        val envVarsPrefix = "$prefix${::envVars.name}."
        project.properties.filterKeys { it.startsWith(envVarsPrefix) }.map { (key, value) ->
            key.removePrefix(envVarsPrefix) to value.toString()
        }.toMap().takeIf { it.isNotEmpty() }?.also {
            envVars = it
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

    private fun <T, O> functionName(ref: KFunction2<O, Action<T>, Unit>) = ref.name
}
