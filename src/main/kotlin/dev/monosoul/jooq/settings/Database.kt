package dev.monosoul.jooq.settings

import org.gradle.api.Action

sealed class Database : JdbcAware, SettingsElement {
    abstract var username: String
    abstract var password: String
    abstract var name: String
    abstract var port: Int
    internal abstract val jdbc: Jdbc

    override fun jdbc(customizer: Action<Jdbc>) = customizer.execute(jdbc)

    data class Internal(
        override var username: String = "postgres",
        override var password: String = "postgres",
        override var name: String = "postgres",
        override var port: Int = 5432,
        override val jdbc: Jdbc = Jdbc(),
    ) : Database() {
        internal fun getJdbcUrl(host: String, port: Int) = "${jdbc.schema}://$host:$port/$name${jdbc.urlQueryParams}"
    }

    data class External(
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
