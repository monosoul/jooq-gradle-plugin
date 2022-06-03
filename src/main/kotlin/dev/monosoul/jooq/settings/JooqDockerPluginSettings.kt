package dev.monosoul.jooq.settings

import dev.monosoul.jooq.container.GenericDatabaseContainer
import org.gradle.api.provider.Provider
import java.io.Serializable
import java.net.URLClassLoader

sealed class JooqDockerPluginSettings : Serializable {
    abstract val jdbc: Jdbc
    abstract val database: Database
    internal abstract fun runWithDatabaseCredentials(
        classloaderProvider: Provider<URLClassLoader>,
        block: (URLClassLoader, DatabaseCredentials) -> Unit
    )

    fun jdbc(block: Jdbc.() -> Unit) = jdbc.apply(block)

    class WithContainer private constructor(
        override val jdbc: Jdbc,
        override val database: Database.Internal,
        val image: Image,
    ) : JooqDockerPluginSettings() {
        override fun runWithDatabaseCredentials(
            classloaderProvider: Provider<URLClassLoader>,
            block: (URLClassLoader, DatabaseCredentials) -> Unit
        ) {
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

            try {
                block(
                    jdbcAwareClassloader,
                    DatabaseCredentials(
                        jdbcDriverClassName = jdbc.driverClassName,
                        jdbcUrl = dbContainer.jdbcUrl,
                        username = dbContainer.username,
                        password = dbContainer.password,
                    )
                )
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
    ) : JooqDockerPluginSettings() {
        private fun getJdbcUrl() =
            "${jdbc.schema}://${database.host}:${database.port}/${database.name}${jdbc.urlQueryParams}"

        override fun runWithDatabaseCredentials(
            classloaderProvider: Provider<URLClassLoader>,
            block: (URLClassLoader, DatabaseCredentials) -> Unit
        ) {
            block(
                classloaderProvider.get(),
                DatabaseCredentials(
                    jdbcDriverClassName = jdbc.driverClassName,
                    jdbcUrl = getJdbcUrl(),
                    username = database.username,
                    password = database.password
                )
            )
        }

        fun db(block: Database.External.() -> Unit) = database.apply(block)

        companion object {
            fun new(block: WithoutContainer.() -> Unit = {}) = WithoutContainer(
                jdbc = Jdbc(),
                database = Database.External(),
            ).apply(block)
        }
    }
}
