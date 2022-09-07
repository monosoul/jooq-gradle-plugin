package dev.monosoul.jooq.settings

import org.gradle.api.Action
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Internal as GradleInternal

sealed class Database : JdbcAware, SettingsElement {
    @get:Input
    abstract var username: String

    @get:Input
    abstract var password: String

    @get:Input
    abstract var name: String

    @get:Input
    abstract var port: Int

    @get:Nested
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
        @get:Input var host: String = "localhost",
        override var port: Int = 5432,
        override val jdbc: Jdbc = Jdbc(),
    ) : Database() {
        @GradleInternal
        internal fun getJdbcUrl() = "${jdbc.schema}://$host:$port/$name${jdbc.urlQueryParams}"
    }
}
