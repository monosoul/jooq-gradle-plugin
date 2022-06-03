package dev.monosoul.jooq

import dev.monosoul.jooq.settings.JooqDockerPluginSettings
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import org.gradle.api.Action
import java.io.Serializable

open class JooqExtension : Serializable {
    internal var pluginSettings: JooqDockerPluginSettings = WithContainer.new()

    fun withContainer(configure: Action<WithContainer>) {
        pluginSettings = WithContainer.new {
            configure.execute(this)
        }
    }

    fun withoutContainer(configure: Action<WithoutContainer>) {
        pluginSettings = WithoutContainer.new {
            configure.execute(this)
        }
    }
}

