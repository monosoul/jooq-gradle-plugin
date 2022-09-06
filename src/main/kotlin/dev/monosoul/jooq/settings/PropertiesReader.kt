package dev.monosoul.jooq.settings

import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import org.gradle.api.Action
import kotlin.reflect.KFunction2
import kotlin.reflect.KMutableProperty0

internal object PropertiesReader {
    const val PREFIX = "dev.monosoul.jooq."
    private val WITH_CONTAINER = "${PREFIX}${functionName(SettingsAware::withContainer)}."
    private val WITHOUT_CONTAINER = "${PREFIX}${functionName(SettingsAware::withoutContainer)}."
    private val IMAGE_PREFIX = "${functionName(ImageAware::image)}."
    private val DATABASE_PREFIX = "${functionName(DbAware<Database>::db)}."
    private val JDBC_PREFIX = "${functionName(JdbcAware::jdbc)}."

    fun WithContainer.applyPropertiesFrom(pluginProperties: Map<String, String>): JooqDockerPluginSettings =
        if (pluginProperties.keys.any { it.startsWith(WITHOUT_CONTAINER) }) {
            WithoutContainer {
                applyPropertiesFrom(pluginProperties)
            }
        } else {
            onlyApplyPropertiesFrom(pluginProperties)
        }

    private fun WithContainer.onlyApplyPropertiesFrom(pluginProperties: Map<String, String>) = apply {
        image.applyPropertiesFrom(pluginProperties)
        database.applyPropertiesFrom(pluginProperties)
    }

    fun WithoutContainer.applyPropertiesFrom(pluginProperties: Map<String, String>): JooqDockerPluginSettings = apply {
        database.applyPropertiesFrom(pluginProperties)
    }

    private fun Jdbc.applyPropertiesFrom(pluginProperties: Map<String, String>, namespace: String) {
        val prefix = "$namespace$JDBC_PREFIX"
        pluginProperties.findAndSetProperty(prefix, ::schema)
        pluginProperties.findAndSetProperty(prefix, ::driverClassName)
        pluginProperties.findAndSetProperty(prefix, ::urlQueryParams)
    }

    private fun Database.External.applyPropertiesFrom(pluginProperties: Map<String, String>) {
        val prefix = "$WITHOUT_CONTAINER$DATABASE_PREFIX"
        pluginProperties.findAndSetProperty(prefix, ::host)
        pluginProperties.findAndSetProperty(prefix, ::port) { it.toInt() }
        pluginProperties.findAndSetProperty(prefix, ::name)
        pluginProperties.findAndSetProperty(prefix, ::username)
        pluginProperties.findAndSetProperty(prefix, ::password)
        jdbc.applyPropertiesFrom(pluginProperties, prefix)
    }

    private fun Database.Internal.applyPropertiesFrom(pluginProperties: Map<String, String>) {
        val prefix = "$WITH_CONTAINER$DATABASE_PREFIX"
        pluginProperties.findAndSetProperty(prefix, ::port) { it.toInt() }
        pluginProperties.findAndSetProperty(prefix, ::name)
        pluginProperties.findAndSetProperty(prefix, ::username)
        pluginProperties.findAndSetProperty(prefix, ::password)
        jdbc.applyPropertiesFrom(pluginProperties, prefix)
    }

    private fun Image.applyPropertiesFrom(pluginProperties: Map<String, String>) {
        val prefix = "$WITH_CONTAINER$IMAGE_PREFIX"
        pluginProperties.findAndSetProperty(prefix, ::name)
        pluginProperties.findAndSetProperty(prefix, ::command)
        pluginProperties.findAndSetProperty(prefix, ::testQuery)

        val envVarsPrefix = "$prefix${::envVars.name}."
        pluginProperties.filterKeys { it.startsWith(envVarsPrefix) }.map { (key, value) ->
            key.removePrefix(envVarsPrefix) to value.toString()
        }.takeIf { it.isNotEmpty() }?.also {
            envVars = it.toMap()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Map<String, String>.findAndSetProperty(
        prefix: String,
        property: KMutableProperty0<T>,
        mapper: (String) -> T = { it as T }
    ) {
        get("$prefix${property.name}")?.also {
            property.set(mapper(it))
        }
    }

    private fun <T, O> functionName(ref: KFunction2<O, Action<T>, Unit>) = ref.name
}
