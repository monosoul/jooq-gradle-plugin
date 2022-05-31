package dev.monosoul.jooq

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import java.net.URI
import kotlin.streams.asStream

class DatabaseHostResolverTest {

    private val dbHostResolver = DatabaseHostResolver(null)

    @TestFactory
    fun `should resolve database host based on docker host`() = sequenceOf(
        "http://hostname:6789" to "hostname",
        "https://fancyhost:6789" to "fancyhost",
        "tcp://another:6789" to "another",
        "unix:///var/run/docker.sock" to "localhost",
        "npipe:////./pipe/docker_engine" to "localhost",
    ).map { (uri, host) ->
        dynamicTest("should resolve $uri to $host") {
            expectThat(dbHostResolver.resolveHost(URI(uri))) isEqualTo host
        }
    }.asStream()

    @TestFactory
    fun `should throw IllegalStateException when unable to resolve database host from docker host`() = sequenceOf(
        "unknown://host",
        "host",
    ).map { uri ->
        dynamicTest("should throw IllegalStateException when unable to resolve $uri") {
            expectThrows<IllegalStateException> {
                dbHostResolver.resolveHost(URI(uri))
            }
        }
    }.asStream()

    @Test
    fun `should override database host when override provided`() {
        // given
        val dbHostOverride = "override"
        val localDbResolver = DatabaseHostResolver(dbHostOverride)

        // when
        val actual = localDbResolver.resolveHost(URI("http://localhost:8080"))

        // then
        expectThat(actual) isEqualTo dbHostOverride
    }

    @Test
    fun `should not throw exception when database host cannot be resolved, but override was provided`() {
        // given
        val dbHostOverride = "override"
        val localDbResolver = DatabaseHostResolver(dbHostOverride)

        // when
        val actual = localDbResolver.resolveHost(URI("unknown://host"))

        // then
        expectThat(actual) isEqualTo dbHostOverride
    }
}
