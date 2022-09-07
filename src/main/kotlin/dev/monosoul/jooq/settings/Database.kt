package dev.monosoul.jooq.settings

import org.gradle.api.Action
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Internal as GradleInternal

sealed class Database : JdbcAware, SettingsElement {
    abstract var username: String
    abstract var password: String
    abstract var name: String
    abstract var port: Int
    internal abstract val jdbc: Jdbc

    override fun jdbc(customizer: Action<Jdbc>) = customizer.execute(jdbc)

    data class Internal(
        @get:Input override var username: String = "postgres",
        @get:Input override var password: String = "postgres",
        @get:Input override var name: String = "postgres",
        @get:Input override var port: Int = 5432,
        @get:Nested override val jdbc: Jdbc = Jdbc(),
    ) : Database() {
        @GradleInternal
        internal fun getJdbcUrl(host: String, port: Int) = "${jdbc.schema}://$host:$port/$name${jdbc.urlQueryParams}"
    }

    data class External(
        @get:Input override var username: String = "postgres",
        @get:Input override var password: String = "postgres",
        @get:Input override var name: String = "postgres",
        @get:Input var host: String = "localhost",
        @get:Input override var port: Int = 5432,
        @get:Nested override val jdbc: Jdbc = Jdbc(),
    ) : Database() {
        @GradleInternal
        internal fun getJdbcUrl() = "${jdbc.schema}://$host:$port/$name${jdbc.urlQueryParams}"
    }
}
