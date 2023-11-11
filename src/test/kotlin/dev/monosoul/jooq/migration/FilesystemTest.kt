package dev.monosoul.jooq.migration

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.withElementAt
import kotlin.streams.asStream

class FilesystemTest {
    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
    }

    @AfterEach
    fun tearDown() {
        project.rootDir.deleteRecursively()
    }

    @Test
    fun `given a files collection, when getting locations, then should prefix them all with filesystem`() {
        // given
        val paths = project.files("someDir", "someOtherDir")

        // when
        val actual = MigrationLocation.Filesystem(paths).locations

        // then
        expectThat(actual) hasSize 2 and {
            withElementAt(0) {
                contains("^filesystem:.*someDir$".toRegex())
            }
            withElementAt(1) {
                contains("^filesystem:.*someOtherDir$".toRegex())
            }
        }
    }

    @TestFactory
    fun `given a files collection, when getting path, then return the collection as is`() =
        sequenceOf(
            "multiple dirs" to project.files("someDir", "someOtherDir"),
            "single dir" to project.files("someDir"),
            "project dir" to project.files(),
        ).map { (description, paths) ->
            dynamicTest("given $description, should return path as is") {
                // when
                val actual = MigrationLocation.Filesystem(paths).path

                // then
                expectThat(actual) isEqualTo paths
            }
        }.asStream()

    @TestFactory
    fun `given a files collection, when getting extraClasspath, then return an empty list`() =
        sequenceOf(
            "multiple dirs" to project.files("someDir", "someOtherDir"),
            "single dir" to project.files("someDir"),
            "project dir" to project.files(),
        ).map { (description, paths) ->
            dynamicTest("given $description, should return empty extra classpath") {
                // when
                val actual = MigrationLocation.Filesystem(paths).extraClasspath()

                // then
                expectThat(actual).isEmpty()
            }
        }.asStream()
}
