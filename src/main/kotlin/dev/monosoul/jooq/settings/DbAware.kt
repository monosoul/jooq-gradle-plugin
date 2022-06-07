package dev.monosoul.jooq.settings

import dev.monosoul.jooq.util.callWith
import groovy.lang.Closure
import org.gradle.api.Action

internal interface DbAware<T : Database> {
    /**
     * Configure the database settings.
     */
    fun db(customizer: Action<T>)

    /**
     * Configure the database settings.
     */
    fun db(closure: Closure<T>) = db(closure::callWith)
}
