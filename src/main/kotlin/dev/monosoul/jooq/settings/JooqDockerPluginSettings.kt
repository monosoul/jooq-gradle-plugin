package dev.monosoul.jooq.settings

import dev.monosoul.jooq.callWith
import dev.monosoul.jooq.container.GenericDatabaseContainer
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.provider.Provider
import java.io.Serializable
import java.net.URLClassLoader

sealed class JooqDockerPluginSettings : Serializable {
    abstract val database: Database
    internal abstract fun runWithDatabaseCredentials(
        classloaderProvider: Provider<URLClassLoader>,
        block: (URLClassLoader, DatabaseCredentials) -> Unit
    )

    class WithContainer(customizer: Action<WithContainer> = Action<WithContainer> { }) : JooqDockerPluginSettings() {
        override val database = Database.Internal()
        val image = Image(database)

        init {
            customizer.execute(this)
        }

        override fun runWithDatabaseCredentials(
            classloaderProvider: Provider<URLClassLoader>,
            block: (URLClassLoader, DatabaseCredentials) -> Unit
        ) {
            val jdbcAwareClassloader = classloaderProvider.get()
            val dbContainer = GenericDatabaseContainer(
                imageName = image.name,
                env = image.envVars,
                testQueryString = image.testQuery,
                database = database,
                jdbcAwareClassLoader = jdbcAwareClassloader,
                command = image.command,
            ).also { it.start() }

            try {
                block(
                    jdbcAwareClassloader,
                    DatabaseCredentials(
                        jdbcDriverClassName = dbContainer.driverClassName,
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
    }

    class WithoutContainer(
        customizer: Action<WithoutContainer> = Action<WithoutContainer> { }
    ) : JooqDockerPluginSettings() {
        override val database = Database.External()

        init {
            customizer.execute(this)
        }

        override fun runWithDatabaseCredentials(
            classloaderProvider: Provider<URLClassLoader>,
            block: (URLClassLoader, DatabaseCredentials) -> Unit
        ) {
            block(
                classloaderProvider.get(),
                DatabaseCredentials(
                    jdbcDriverClassName = database.jdbc.driverClassName,
                    jdbcUrl = database.getJdbcUrl(),
                    username = database.username,
                    password = database.password
                )
            )
        }

        fun db(customizer: Action<Database.External>) = customizer.execute(database)
        fun db(closure: Closure<Database.External>) = db(closure::callWith)
    }
}
