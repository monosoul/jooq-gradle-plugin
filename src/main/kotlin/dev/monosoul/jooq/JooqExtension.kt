package dev.monosoul.jooq

import dev.monosoul.jooq.settings.JooqDockerPluginSettings
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import dev.monosoul.jooq.settings.PropertiesReader.applyPropertiesFrom
import dev.monosoul.jooq.settings.SettingsAware
import org.gradle.api.Action
import org.gradle.api.provider.Provider
import java.io.Serializable

open class JooqExtension(
    private val propertiesProvider: Provider<Map<String, String>>,
) : Serializable, SettingsAware {
    internal var pluginSettings: Provider<JooqDockerPluginSettings> = propertiesProvider.map {
        WithContainer().applyPropertiesFrom(it)
    }

    @Suppress("unused")
    override fun withContainer(configure: Action<WithContainer>) {
        pluginSettings = propertiesProvider.map { WithContainer(configure).applyPropertiesFrom(it) }
    }

    @Suppress("unused")
    override fun withoutContainer(configure: Action<WithoutContainer>) {
        pluginSettings = propertiesProvider.map { WithoutContainer(configure).applyPropertiesFrom(it) }
    }
}

