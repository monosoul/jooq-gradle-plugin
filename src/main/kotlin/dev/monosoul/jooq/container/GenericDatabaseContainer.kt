package dev.monosoul.jooq.container

import dev.monosoul.jooq.settings.Database
import dev.monosoul.jooq.settings.Image
import org.slf4j.LoggerFactory
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName
import java.lang.reflect.Field
import java.sql.Driver
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class GenericDatabaseContainer(
    private val image: Image,
    private val database: Database.Internal,
    private val jdbcAwareClassLoader: ClassLoader,
) : JdbcDatabaseContainer<GenericDatabaseContainer>(DockerImageName.parse(image.name)) {

    private val driverLoadLock = ReentrantLock()
    private var driver: Driver? by ReflectionDelegate(
        JdbcDatabaseContainer::class.java.getDeclaredField("driver").also {
            it.isAccessible = true
        }
    )

    init {
        withLogConsumer(
            Slf4jLogConsumer(
                LoggerFactory.getLogger("JooqGenerationDb[$dockerImageName]")
            )
        )
        withEnv(image.envVars)
        withExposedPorts(database.port)
        setWaitStrategy(HostPortWaitStrategy())
        image.command?.run(::withCommand)
    }

    override fun getDriverClassName() = database.jdbc.driverClassName

    override fun getJdbcUrl() = database.getJdbcUrl(host, getMappedPort(database.port))

    override fun getUsername() = database.username

    override fun getPassword() = database.password

    override fun getTestQueryString() = image.testQuery

    override fun getDatabaseName() = database.name

    override fun getJdbcDriverInstance(): Driver {
        if (driver == null) {
            driverLoadLock.withLock {
                if (driver == null) {
                    return try {
                        @Suppress("DEPRECATION")
                        jdbcAwareClassLoader.loadClass(driverClassName).newInstance() as Driver
                    } catch (e: Exception) {
                        when (e) {
                            is InstantiationException, is IllegalAccessException, is ClassNotFoundException -> {
                                throw NoDriverFoundException("Could not get Driver", e)
                            }
                            else -> throw e
                        }
                    }.also {
                        driver = it
                    }
                }
            }
        }

        return driver!!
    }

    private class ReflectionDelegate<T>(private val field: Field) : ReadWriteProperty<Any, T> {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: Any, property: KProperty<*>): T = field.get(thisRef) as T
        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) = field.set(thisRef, value)
    }
}
