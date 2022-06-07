package dev.monosoul.jooq.container

import dev.monosoul.jooq.settings.Database
import dev.monosoul.jooq.settings.Image
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.message
import java.sql.Driver
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.streams.asStream

@ExtendWith(MockKExtension::class)
class GenericDatabaseContainerTest {

    private lateinit var image: Image

    private lateinit var database: Database.Internal

    @MockK
    private lateinit var jdbcAwareClassLoader: ClassLoader

    private lateinit var container: GenericDatabaseContainer

    @BeforeEach
    fun setUp() {
        database = Database.Internal()
        image = Image()

        container = GenericDatabaseContainer(
            image = image,
            database = database,
            jdbcAwareClassLoader = jdbcAwareClassLoader,
        )
    }

    @Test
    fun `should only load driver class once`() {
        // given
        every { jdbcAwareClassLoader.loadClass(any()) } returns TestDriver::class.java

        // when
        container.jdbcDriverInstance
        container.jdbcDriverInstance

        // then
        verify(exactly = 1) {
            jdbcAwareClassLoader.loadClass(database.jdbc.driverClassName)
        }
    }

    @TestFactory
    fun `should rethrow expected exceptions as no driver found exception`() = sequenceOf(
        InstantiationException(),
        IllegalAccessException(),
        ClassNotFoundException(),
    ).map { exception ->
        dynamicTest("should rethrow ${exception::class.simpleName} as no driver found exception") {
            // given
            every { jdbcAwareClassLoader.loadClass(any()) } throws exception

            // when && then
            expectThrows<NoDriverFoundException> {
                container.jdbcDriverInstance
            }.message isEqualTo "Could not get Driver"
        }
    }.asStream()

    @Test
    fun `should rethrow unexpected exceptions as is`() {
        // given
        val unexpectedException = RuntimeException()
        every { jdbcAwareClassLoader.loadClass(any()) } throws unexpectedException

        // when && then
        expectThrows<RuntimeException> {
            container.jdbcDriverInstance
        } isEqualTo unexpectedException
    }

    @Test
    fun `should prevent asynchronous driver instantiation`() {
        // given
        val startLatch = CountDownLatch(2)
        val driverGetLatch = CountDownLatch(2)
        val threadPool = Executors.newFixedThreadPool(2)
        every { jdbcAwareClassLoader.loadClass(any()) } answers {
            driverGetLatch.countDown()
            TestDriver::class.java
        }

        // when
        val futures = (1..2).map {
            threadPool.submit {
                startLatch.countDown()
                container.jdbcDriverInstance
            }
        }
        driverGetLatch.countDown()
        futures.forEach { it.get() }
        threadPool.shutdown()

        // then
        verify(exactly = 1) {
            jdbcAwareClassLoader.loadClass(database.jdbc.driverClassName)
        }
    }

    @Test
    fun `should delegate getDatabaseName method to the database object as is`() {
        // when
        val actual = container.databaseName

        // then
        expectThat(actual) isEqualTo database.name
    }

    @Test
    fun `should delegate getDriverClassName method to the jdbc object as is`() {
        // when
        val actual = container.driverClassName

        // then
        expectThat(actual) isEqualTo database.jdbc.driverClassName
    }

    @Test
    fun `should delegate getUsername method to the database object as is`() {
        // when
        val actual = container.username

        // then
        expectThat(actual) isEqualTo database.username
    }

    @Test
    fun `should delegate getPassword method to the database object as is`() {
        // when
        val actual = container.password

        // then
        expectThat(actual) isEqualTo database.password
    }

    private class TestDriver(val mockDriver: Driver = mockk()) : Driver by mockDriver
}
