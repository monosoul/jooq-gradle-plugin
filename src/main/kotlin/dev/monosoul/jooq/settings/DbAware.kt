package dev.monosoul.jooq.settings

import dev.monosoul.jooq.callWith
import groovy.lang.Closure
import org.gradle.api.Action

internal interface DbAware<T : Database> {
    fun db(customizer: Action<T>)
    fun db(closure: Closure<T>) = db(closure::callWith)
}
