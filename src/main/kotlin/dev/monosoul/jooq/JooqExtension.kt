package dev.monosoul.jooq

import dev.monosoul.jooq.settings.JooqDockerPluginSettings
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import groovy.lang.Closure
import org.gradle.api.Action
import java.io.Serializable

open class JooqExtension : Serializable {
    internal var pluginSettings: JooqDockerPluginSettings = WithContainer.new()

    @Suppress("unused")
    fun withContainer(configure: Action<WithContainer>) {
        pluginSettings = WithContainer.new(configure)
    }

    @Suppress("unused")
    fun withContainer(closure: Closure<WithContainer>) = withContainer(closure::callWith)

    @Suppress("unused")
    fun withoutContainer(configure: Action<WithoutContainer>) {
        pluginSettings = WithoutContainer.new(configure)
    }

    @Suppress("unused")
    fun withoutContainer(closure: Closure<WithoutContainer>) = withoutContainer(closure::callWith)
}

