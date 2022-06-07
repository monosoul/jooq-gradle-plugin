package dev.monosoul.jooq.settings

import dev.monosoul.jooq.util.callWith
import groovy.lang.Closure
import org.gradle.api.Action

internal interface JdbcAware {
    /**
     * Configures the JDBC connection settings.
     */
    fun jdbc(customizer: Action<Jdbc>)

    /**
     * Configures the JDBC connection settings.
     */
    fun jdbc(closure: Closure<Jdbc>) = jdbc(closure::callWith)
}
