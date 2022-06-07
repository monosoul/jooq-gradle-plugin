package dev.monosoul.jooq.settings

import dev.monosoul.jooq.util.callWith
import groovy.lang.Closure
import org.gradle.api.Action

internal interface ImageAware {
    /**
     * Configure the Docker image settings.
     */
    fun image(customizer: Action<Image>)

    /**
     * Configure the Docker image settings.
     */
    fun image(closure: Closure<Image>) = image(closure::callWith)
}
