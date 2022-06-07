package dev.monosoul.jooq.settings

import dev.monosoul.jooq.container.GenericDatabaseContainer
import dev.monosoul.jooq.util.CodegenClasspathAwareClassLoaders
import org.gradle.api.Action
import java.io.Serializable

sealed class JooqDockerPluginSettings : Serializable {
    internal abstract val database: Database
    internal abstract fun runWithDatabaseCredentials(
        classloaders: CodegenClasspathAwareClassLoaders,
        block: (CodegenClasspathAwareClassLoaders, DatabaseCredentials) -> Unit
    )

    internal abstract fun copy(): JooqDockerPluginSettings

    class WithContainer private constructor(
        override val database: Database.Internal,
        internal val image: Image,
    ) : JooqDockerPluginSettings(), DbAware<Database.Internal>, ImageAware {
        private constructor(database: Database.Internal) : this(database, Image(database))
        constructor(customizer: Action<WithContainer> = Action<WithContainer> { }) : this(Database.Internal()) {
            customizer.execute(this)
        }

        override fun runWithDatabaseCredentials(
            classloaders: CodegenClasspathAwareClassLoaders,
            block: (CodegenClasspathAwareClassLoaders, DatabaseCredentials) -> Unit
        ) {
            val dbContainer = GenericDatabaseContainer(
                image = image,
                database = database,
                jdbcAwareClassLoader = classloaders.buildscriptInclusive,
            ).also { it.start() }

            try {
                block(
                    classloaders,
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

        override fun copy(): WithContainer = WithContainer(
            database = database.run { copy(jdbc = jdbc.copy()) },
            image = image.copy()
        )

        override fun db(customizer: Action<Database.Internal>) = customizer.execute(database)
        override fun image(customizer: Action<Image>) = customizer.execute(image)
    }

    class WithoutContainer private constructor(
        override val database: Database.External
    ) : JooqDockerPluginSettings(), DbAware<Database.External> {
        constructor(customizer: Action<WithoutContainer> = Action<WithoutContainer> { }) : this(Database.External()) {
            customizer.execute(this)
        }

        override fun runWithDatabaseCredentials(
            classloaders: CodegenClasspathAwareClassLoaders,
            block: (CodegenClasspathAwareClassLoaders, DatabaseCredentials) -> Unit
        ) {
            block(
                classloaders,
                DatabaseCredentials(
                    jdbcDriverClassName = database.jdbc.driverClassName,
                    jdbcUrl = database.getJdbcUrl(),
                    username = database.username,
                    password = database.password
                )
            )
        }

        override fun copy(): WithoutContainer = WithoutContainer(
            database = database.run { copy(jdbc = jdbc.copy()) }
        )

        override fun db(customizer: Action<Database.External>) = customizer.execute(database)
    }
}
