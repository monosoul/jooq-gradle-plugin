package dev.monosoul.jooq

import dev.monosoul.GenericDatabaseContainer
import dev.monosoul.jooq.JooqExtension.Image
import dev.monosoul.jooq.JooqExtension.Jdbc
import org.gradle.api.provider.Provider
import java.io.Serializable
import java.net.URLClassLoader

sealed class JooqDockerPluginSettings : Serializable {
    abstract val jdbc: Jdbc
    abstract val database: Database
    internal abstract fun withDatabaseCredentials(classloaderProvider: Provider<URLClassLoader>, block: (URLClassLoader, JdbcDriverClassName, JdbcUrl, Username, Password) -> Unit)

    fun jdbc(block: Jdbc.() -> Unit) = jdbc.apply(block)

    class WithContainer private constructor(
        override val jdbc: Jdbc,
        override val database: Database.Internal,
        val image: Image,
    ) : JooqDockerPluginSettings(), Serializable {
        override fun withDatabaseCredentials(classloaderProvider: Provider<URLClassLoader>, block: (URLClassLoader, JdbcDriverClassName, JdbcUrl, Username, Password) -> Unit) {
            val jdbcAwareClassloader = classloaderProvider.get()
            val dbContainer = GenericDatabaseContainer(
                imageName = image.getImageName(),
                env = image.envVars.mapValues { (_, value) -> value.toString() },
                testQueryString = image.testQuery,
                database = database,
                jdbc = jdbc,
                jdbcAwareClassLoader = jdbcAwareClassloader,
                command = image.command,
            ).also { it.start() }
            val jdbcUrl = dbContainer.jdbcUrl
            try {
                block(jdbcAwareClassloader, JdbcDriverClassName(jdbc.driverClassName), JdbcUrl(jdbcUrl), Username(dbContainer.username), Password(dbContainer.password))
            } finally {
                dbContainer.stop()
            }
        }

        fun db(block: Database.Internal.() -> Unit) = database.apply(block)
        fun image(block: Image.() -> Unit) = image.apply(block)

        companion object {
            fun new(block: WithContainer.() -> Unit = {}): WithContainer {
                val database = Database.Internal()
                return WithContainer(
                    jdbc = Jdbc(),
                    database = database,
                    image = Image(database),
                ).apply(block)
            }
        }
    }

    class WithoutContainer private constructor(
        override val jdbc: Jdbc,
        override val database: Database.External,
    ) : JooqDockerPluginSettings(), Serializable {
        private fun getJdbcUrl() =
            "${jdbc.schema}://${database.host}:${database.port}/${database.name}${jdbc.urlQueryParams}"

        override fun withDatabaseCredentials(classloaderProvider: Provider<URLClassLoader>, block: (URLClassLoader, JdbcDriverClassName, JdbcUrl, Username, Password) -> Unit) {
            block(classloaderProvider.get(), JdbcDriverClassName(jdbc.driverClassName), JdbcUrl(getJdbcUrl()), Username(database.username), Password(database.password))
        }

        fun db(block: Database.External.() -> Unit) = database.apply(block)

        companion object {
            fun new(block: WithoutContainer.() -> Unit = {}) = WithoutContainer(
                jdbc = Jdbc(),
                database = Database.External(),
            ).apply(block)
        }
    }

    sealed class Database : Serializable {
        abstract var username: String
        abstract var password: String
        abstract var name: String
        abstract var port: Int

        class Internal(
            override var username: String = "postgres",
            override var password: String = "postgres",
            override var name: String = "postgres",
            override var port: Int = 5432,
        ) : Database(), Serializable

        class External(
            override var username: String = "postgres",
            override var password: String = "postgres",
            override var name: String = "postgres",
            var host: String = "localhost",
            override var port: Int = 5432,
        ) : Database(), Serializable
    }

    internal data class JdbcDriverClassName(val value: String) : Serializable
    internal data class JdbcUrl(val value: String) : Serializable
    internal data class Username(val value: String) : Serializable
    internal data class Password(val value: String) : Serializable
}
