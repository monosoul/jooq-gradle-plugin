package dev.monosoul.jooq

import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import groovy.lang.Closure
import org.gradle.api.Action

internal interface SettingsAware {
    @Suppress("unused")
    fun withContainer(configure: Action<WithContainer>)

    @Suppress("unused")
    fun withContainer(closure: Closure<WithContainer>) = withContainer(closure::callWith)

    @Suppress("unused")
    fun withoutContainer(configure: Action<WithoutContainer>)

    @Suppress("unused")
    fun withoutContainer(closure: Closure<WithoutContainer>) = withoutContainer(closure::callWith)
}
