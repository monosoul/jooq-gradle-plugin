package dev.monosoul.jooq

import dev.monosoul.jooq.settings.JooqDockerPluginSettings
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import groovy.lang.Closure
import org.gradle.api.Action
import java.io.Serializable

open class JooqExtension : Serializable {
    internal var pluginSettings: JooqDockerPluginSettings = WithContainer.new()

    fun withContainer(configure: Action<WithContainer>) {
        pluginSettings = WithContainer.new(configure)
    }

    fun withContainer(closure: Closure<WithContainer>) = withContainer(closure::callWith)

    fun withoutContainer(configure: Action<WithoutContainer>) {
        pluginSettings = WithoutContainer.new(configure)
    }

    fun withoutContainer(closure: Closure<WithoutContainer>) = withoutContainer(closure::callWith)
}

