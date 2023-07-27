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
import strikt.assertions.endsWith
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.withElementAt
import kotlin.streams.asStream

class ClasspathTest {

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
    fun `given a list of classpath locations, when getting locations, then should prefix them all with classpath`() {
        // given
        val classpathLocations = listOf("/someDir", "/someOtherDir")

        // when
        val actual = MigrationLocation.Classpath(project.files(), classpathLocations).locations

        // then
        expectThat(actual) hasSize 2 and {
            withElementAt(0) {
                isEqualTo("classpath:/someDir")
            }
            withElementAt(1) {
                isEqualTo("classpath:/someOtherDir")
            }
        }
    }

    @TestFactory
    fun `given a files collection, when getting path, then return the collection as is`() = sequenceOf(
        "multiple dirs" to project.files("someDir", "someOtherDir"),
        "single dir" to project.files("someDir"),
        "project dir" to project.files(),
    ).map { (description, paths) ->
        dynamicTest("given $description, should return path as is") {
            // when
            val actual = MigrationLocation.Classpath(paths)

            // then
            expectThat(actual) {
                get { path } isEqualTo paths
                get { locations } isEqualTo listOf("classpath:/db/migration")
            }
        }
    }.asStream()

    @Test
    fun `given a file collection, when getting locations, then should prefix them all with classpath`() {
        // given
        project.mkdir("someDir")
        project.file("someDir/some.jar").createNewFile()
        val paths = project.files("someDir", "someOtherDir")

        // when
        val actual = MigrationLocation.Classpath(paths).extraClasspath()

        // then
        expectThat(actual) hasSize 3 and {
            withElementAt(0) {
                get { path }.contains("someDir/*$".toRegex())
            }
            withElementAt(1) {
                get { path }.endsWith("some.jar")
            }
            withElementAt(2) {
                get { path }.contains("someOtherDir/*$".toRegex())
            }
        }
    }
}
