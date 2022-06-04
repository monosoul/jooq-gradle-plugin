package dev.monosoul.jooq

import dev.monosoul.jooq.settings.JooqDockerPluginSettings
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import org.gradle.api.Action
import java.io.Serializable

open class JooqExtension : Serializable, SettingsAware {
    internal var pluginSettings: JooqDockerPluginSettings = WithContainer()

    @Suppress("unused")
    override fun withContainer(configure: Action<WithContainer>) {
        pluginSettings = WithContainer(configure)
    }

    @Suppress("unused")
    override fun withoutContainer(configure: Action<WithoutContainer>) {
        pluginSettings = WithoutContainer(configure)
    }
}

