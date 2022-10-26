package dev.monosoul.jooq

import dev.monosoul.jooq.settings.JooqDockerPluginSettings
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import dev.monosoul.jooq.settings.PropertiesReader.applyPropertiesFrom
import dev.monosoul.jooq.settings.SettingsAware
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.property
import java.io.Serializable
import javax.inject.Inject

open class JooqExtension @Inject constructor(
    private val propertiesProvider: Provider<Map<String, String>>,
    objectFactory: ObjectFactory,
) : Serializable, SettingsAware {
    internal val pluginSettings: Property<JooqDockerPluginSettings> = objectFactory.property<JooqDockerPluginSettings>()
        .convention(
            propertiesProvider.map {
                WithContainer().applyPropertiesFrom(it)
            }
        )

    @Suppress("unused")
    override fun withContainer(configure: Action<WithContainer>) {
        pluginSettings.set(
            propertiesProvider.map { WithContainer(configure).applyPropertiesFrom(it) }
        )
    }

    @Suppress("unused")
    override fun withoutContainer(configure: Action<WithoutContainer>) {
        pluginSettings.set(
            propertiesProvider.map { WithoutContainer(configure).applyPropertiesFrom(it) }
        )
    }
}

