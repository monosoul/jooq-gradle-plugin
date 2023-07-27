package dev.monosoul.jooq.migration

import dev.monosoul.jooq.migration.MigrationLocation.Classpath
import dev.monosoul.jooq.migration.MigrationLocation.Filesystem
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.net.URL

/**
 * Specification for Flyway migrations location.
 *
 * Can be one of: [Filesystem], [Classpath]
 */
sealed class MigrationLocation {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val path: FileCollection

    @get:Input
    abstract val locations: List<String>

    internal abstract fun extraClasspath(): List<URL>

    /**
     * Location of SQL migrations on a filesystem.
     *
     * E.g.:
     *
     * ```
     * MigrationLocation.Filesystem(project.files("src/main/resources/db/migration"))
     * ```
     *
     * where `project` is Gradle's [Project]
     *
     * @property path Path to the directory with migration SQL scripts on filesystem
     *
     * @constructor Creates an instance of filesystem migration location. See [Filesystem].
     */
    data class Filesystem(
        /**
         * One or multiple locations of migration SQL scripts
         */
        override val path: FileCollection
    ) : MigrationLocation() {

        /**
         * Migration locations in Flyway accepted format.
         *
         * See [FluentConfiguration.locations]
         */
        @get:Internal
        override val locations: List<String> get() = path.map { "${Location.FILESYSTEM_PREFIX}${it.absolutePath}" }
        override fun extraClasspath(): List<URL> = emptyList()
    }

    /**
     * Location of Java-based or SQL migrations to add to Flyway classpath for migration.
     *
     * Can either be a directory with compiled Java-based migrations or a JAR file with Java-based or SQL migrations.
     *
     * E.g.:
     *
     * Using Java-based migrations from a Gradle submodule:
     *
     * ```
     * MigrationLocation.Classpath(
     *    path = project.files(
     *       project(":migrations").sourceSets.main.map { it.output },
     *       // the line below only needed if you use some extra dependencies in your migrations
     *       project(":migrations").sourceSets.main.map { it.runtimeClasspath },
     *    ),
     *    classpathLocation = "/db/migrations"
     * )
     * ```
     *
     * Or if you need to run migrations from a third party JAR file:
     *
     * ```
     * val migrationClasspath by configurations.creating
     *
     * dependencies {
     *    migrationClasspath("third.party:some.artifact:some.version")
     * }
     *
     * tasks.generateJooqClasses {
     *    migrationLocations.set(
     *       MigrationLocation.Classpath(
     *          path = project.files(migrationClasspath),
     *          classpathLocation = "/third/party/package/migrations"
     *       )
     *    )
     * }
     * ```
     *
     * @property path Path to the directory with compiled Java-based migrations or to the JAR file with Java-based migrations. This is the path that will be added to Flyway classpath.
     * @property classpathLocations Location(s) of Java-based migrations within the [path]. For example, the package with migrations.
     *
     * @constructor Creates an instance of classpath migration location. See [Classpath].
     *
     * @see Project.project
     * @see Project.getConfigurations
     * @see Project.files
     */
    data class Classpath(
        override val path: FileCollection,
        private val classpathLocations: List<String>
    ) : MigrationLocation() {
        override val locations: List<String> = classpathLocations.map { "classpath:$it" }

        /**
         * Creates an instance of classpath migration location using a single classpath location. See [Classpath].
         */
        constructor(
            path: FileCollection,
            classpathLocation: String
        ) : this(path, listOf(classpathLocation))

        /**
         * Creates an instance of classpath migration location using default classpath location: `/db/migration`. See [Classpath].
         */
        @Suppress("unused")
        constructor(path: FileCollection) : this(path, "/db/migration")

        override fun extraClasspath(): List<URL> = path.asSequence()
            .flatMap { file ->
                listOf(file).plus(
                    file.listFiles { _, name -> name.endsWith(".jar") }?.asList() ?: emptyList()
                )
            }
            .map { it.toURI().toURL() }
            .toList()
    }
}
