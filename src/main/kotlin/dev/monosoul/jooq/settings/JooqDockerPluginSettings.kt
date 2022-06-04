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

    internal abstract fun copy(): JooqDockerPluginSettings

    class WithContainer private constructor(
        override val database: Database.Internal,
        val image: Image,
    ) : JooqDockerPluginSettings() {
        private constructor(database: Database.Internal) : this(database, Image(database))
        constructor(customizer: Action<WithContainer> = Action<WithContainer> { }) : this(Database.Internal()) {
            customizer.execute(this)
        }

        override fun runWithDatabaseCredentials(
            classloaderProvider: Provider<URLClassLoader>,
            block: (URLClassLoader, DatabaseCredentials) -> Unit
        ) {
            val jdbcAwareClassloader = classloaderProvider.get()
            val dbContainer = GenericDatabaseContainer(
                image = image,
                database = database,
                jdbcAwareClassLoader = jdbcAwareClassloader,
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

        override fun copy(): WithContainer = WithContainer(database.copy(), image.copy())

        fun db(customizer: Action<Database.Internal>) = customizer.execute(database)
        fun db(closure: Closure<Database.Internal>) = db(closure::callWith)
        fun image(customizer: Action<Image>) = customizer.execute(image)
        fun image(closure: Closure<Image>) = image(closure::callWith)
    }

    class WithoutContainer private constructor(
        override val database: Database.External
    ) : JooqDockerPluginSettings() {
        constructor(customizer: Action<WithoutContainer> = Action<WithoutContainer> { }) : this(Database.External()) {
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

        override fun copy(): WithoutContainer = WithoutContainer(database.copy())

        fun db(customizer: Action<Database.External>) = customizer.execute(database)
        fun db(closure: Closure<Database.External>) = db(closure::callWith)
    }
}
