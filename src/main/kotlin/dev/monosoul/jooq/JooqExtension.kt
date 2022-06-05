package dev.monosoul.jooq

import dev.monosoul.jooq.settings.JooqDockerPluginSettings
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import dev.monosoul.jooq.settings.PropertiesReader.settingsFromProperties
import org.gradle.api.Action
import org.gradle.api.Project
import java.io.Serializable
import javax.inject.Inject

open class JooqExtension @Inject constructor(project: Project) : Serializable, SettingsAware {
    internal var pluginSettings: JooqDockerPluginSettings = project.settingsFromProperties()

    @Suppress("unused")
    override fun withContainer(configure: Action<WithContainer>) {
        pluginSettings = WithContainer(configure)
    }

    @Suppress("unused")
    override fun withoutContainer(configure: Action<WithoutContainer>) {
        pluginSettings = WithoutContainer(configure)
    }
}

