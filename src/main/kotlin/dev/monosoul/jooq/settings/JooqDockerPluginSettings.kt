package dev.monosoul.jooq.settings

import dev.monosoul.jooq.callWith
import dev.monosoul.jooq.container.GenericDatabaseContainer
import groovy.lang.Closure
import org.gradle.api.Action
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

    fun jdbc(customizer: Action<Jdbc>) = customizer.execute(jdbc)
    fun jdbc(closure: Closure<Jdbc>) = jdbc(closure::callWith)

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

        fun db(customizer: Action<Database.Internal>) = customizer.execute(database)
        fun db(closure: Closure<Database.Internal>) = db(closure::callWith)
        fun image(customizer: Action<Image>) = customizer.execute(image)
        fun image(closure: Closure<Image>) = image(closure::callWith)

        companion object {
            fun new(customizer: Action<WithContainer> = Action<WithContainer> { }): WithContainer {
                val database = Database.Internal()
                return WithContainer(
                    jdbc = Jdbc(),
                    database = database,
                    image = Image(database),
                ).apply(customizer::execute)
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

        fun db(customizer: Action<Database.External>) = customizer.execute(database)
        fun db(closure: Closure<Database.External>) = db(closure::callWith)

        companion object {
            fun new(customizer: Action<WithoutContainer> = Action<WithoutContainer> { }) = WithoutContainer(
                jdbc = Jdbc(),
                database = Database.External(),
            ).apply(customizer::execute)
        }
    }
}
