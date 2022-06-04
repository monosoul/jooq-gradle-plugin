package dev.monosoul.jooq.settings

import dev.monosoul.jooq.callWith
import groovy.lang.Closure
import org.gradle.api.Action
import java.io.Serializable

sealed class Database : Serializable {
    abstract var username: String
    abstract var password: String
    abstract var name: String
    abstract var port: Int
    abstract val jdbc: Jdbc

    fun jdbc(customizer: Action<Jdbc>) = customizer.execute(jdbc)
    fun jdbc(closure: Closure<Jdbc>) = jdbc(closure::callWith)

    class Internal(
        override var username: String = "postgres",
        override var password: String = "postgres",
        override var name: String = "postgres",
        override var port: Int = 5432,
        override val jdbc: Jdbc = Jdbc(),
    ) : Database() {
        internal fun getJdbcUrl(host: String, port: Int) = "${jdbc.schema}://$host:$port/$name${jdbc.urlQueryParams}"
    }

    class External(
        override var username: String = "postgres",
        override var password: String = "postgres",
        override var name: String = "postgres",
        var host: String = "localhost",
        override var port: Int = 5432,
        override val jdbc: Jdbc = Jdbc(),
    ) : Database() {
        internal fun getJdbcUrl() = "${jdbc.schema}://$host:$port/$name${jdbc.urlQueryParams}"
    }
}
