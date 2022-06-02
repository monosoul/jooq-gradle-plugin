package dev.monosoul

import dev.monosoul.jooq.JooqExtension.Database
import dev.monosoul.jooq.JooqExtension.Jdbc
import org.slf4j.LoggerFactory
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName
import java.lang.reflect.Field
import java.sql.Driver

class GenericDatabaseContainer(
    imageName: String,
    env: Map<String, String>,
    private val database: Database,
    private val jdbc: Jdbc,
    private val jdbcAwareClassLoader: ClassLoader,
    private val testQueryString: String,
    command: String? = null
) : JdbcDatabaseContainer<GenericDatabaseContainer>(DockerImageName.parse(imageName)) {

    private val DRIVER_LOAD_MUTEX = Any()
    private val driverField: Field
    private var driver: Driver?
        get() = driverField.get(this) as Driver?
        set(value) {
            driverField.set(this, value)
        }

    init {
        withLogConsumer(
            Slf4jLogConsumer(
                LoggerFactory.getLogger("JooqGenerationDb[$dockerImageName]")
            )
        )
        withEnv(env)
        withExposedPorts(database.port)
        setWaitStrategy(HostPortWaitStrategy())
        command?.run(::withCommand)
        driverField = JdbcDatabaseContainer::class.java.getDeclaredField("driver").also {
            it.isAccessible = true
        }
    }

    override fun getDriverClassName() = jdbc.driverClassName

    override fun getJdbcUrl() = "${jdbc.schema}://$host:${getMappedPort(database.port)}/$databaseName${jdbc.urlQueryParams}".also {
        logger().info("JdbcUrl: $it")
    }

    override fun getUsername() = database.username

    override fun getPassword() = database.password

    override fun getTestQueryString() = testQueryString

    override fun getDatabaseName() = database.name

    override fun getJdbcDriverInstance(): Driver {
        if (driver == null) {
            synchronized(DRIVER_LOAD_MUTEX) {
                if (driver == null) {
                    return try {
                        jdbcAwareClassLoader.loadClass(this.driverClassName).newInstance() as Driver
                    } catch (e: Exception) {
                        when (e) {
                            is InstantiationException, is IllegalAccessException, is ClassNotFoundException -> {
                                throw NoDriverFoundException("Could not get Driver", e)
                            }
                            else -> throw e
                        }
                    }.also {
                        driverField.set(this, it)
                    }
                }
            }
        }

        return driver!!
    }
}
