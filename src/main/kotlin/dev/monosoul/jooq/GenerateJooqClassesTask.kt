package dev.monosoul.jooq

import dev.monosoul.jooq.codegen.ConfigurationProvider
import dev.monosoul.jooq.codegen.ConfigurationProvider.Companion.postProcess
import dev.monosoul.jooq.codegen.UniversalJooqCodegenRunner
import dev.monosoul.jooq.migration.MigrationLocation
import dev.monosoul.jooq.migration.SchemaVersion
import dev.monosoul.jooq.migration.UniversalMigrationRunner
import dev.monosoul.jooq.settings.DatabaseCredentials
import dev.monosoul.jooq.settings.JooqDockerPluginSettings
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import dev.monosoul.jooq.settings.SettingsAware
import dev.monosoul.jooq.util.CodegenClasspathAwareClassLoaders
import dev.monosoul.jooq.util.callWith
import dev.monosoul.jooq.util.getCodegenLogging
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Generator
import javax.inject.Inject
import org.gradle.api.artifacts.Configuration as GradleConfiguration

@CacheableTask
open class GenerateJooqClassesTask @Inject constructor(
    objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory,
    private val fsOperations: FileSystemOperations,
    private val projectLayout: ProjectLayout,
) : DefaultTask(), SettingsAware {
    /**
     * List of schemas to take into account when running migrations and generating code.
     */
    @Input
    val schemas = objectFactory.listProperty<String>().convention(listOf("public"))

    /**
     * Base package for generated classes.
     */
    @Input
    val basePackageName = objectFactory.property<String>().convention("org.jooq.generated")

    /**
     * Flyway configuration.
     */
    @Input
    val flywayProperties = objectFactory.mapProperty<String, String>().convention(emptyMap())

    /**
     * List of schemas to not generate schema information for (generate classes as for default schema).
     */
    @Input
    val outputSchemaToDefault = objectFactory.setProperty<String>().convention(emptySet())

    /**
     * Map of schema name to specific package name.
     */
    @Input
    val schemaToPackageMapping = objectFactory.mapProperty<String, String>().convention(emptyMap())

    /**
     * Include Flyway migration history table to generated code.
     */
    @Input
    val includeFlywayTable = objectFactory.property<Boolean>().convention(false)

    private val _generatorConfig = objectFactory.property<PrivateValueHolder<Configuration>>().convention(
        providerFactory.provider {
            configurationProvider.defaultConfiguration()
        }.map(::PrivateValueHolder)
    )

    /**
     * Use [usingJavaConfig] or [usingXmlConfig] to provide configuration.
     */
    @get:Input
    @Suppress("unused")
    val generatorConfig: Property<out ValueHolder<Configuration>> get() = _generatorConfig

    /**
     * Location of Flyway migrations to use for code generation.
     *
     * Can be:
     * - [MigrationLocation.Filesystem]:
     *    - directory with SQL migrations
     * - [MigrationLocation.Classpath]:
     *    - directory with Java-based migrations (compiled classes)
     *    - directory with JAR files having Java-based or SQL migrations
     *    - path to a single JAR file having Java-based or SQL migrations
     *
     * Default: "src/main/resources/db/migration" directory of current project.
     *
     * @see MigrationLocation
     */
    @Nested
    val migrationLocations = objectFactory.listProperty<MigrationLocation>().convention(
        listOf(
            MigrationLocation.Filesystem(objectFactory.fileCollection().from("src/main/resources/db/migration"))
        )
    )

    /**
     * Location of generated classes.
     */
    @OutputDirectory
    val outputDirectory =
        objectFactory.directoryProperty().convention(projectLayout.buildDirectory.dir("generated-jooq"))

    /**
     * Classpath for code generation. Derived from jooqCodegen configuration.
     */
    @Classpath
    val codegenClasspath = objectFactory.fileCollection().from(
        project.configurations.named(JooqDockerPlugin.CONFIGURATION_NAME)
    )

    private val localPluginSettings = objectFactory.property<JooqDockerPluginSettings>()

    private val globalPluginSettings = project.extensions.getByType<JooqExtension>().pluginSettings

    private val _pluginSettings: Provider<PrivateValueHolder<JooqDockerPluginSettings>>
        get() = localPluginSettings.orElse(globalPluginSettings).map(::PrivateValueHolder)

    /**
     * Use [withContainer] or [withoutContainer] to provide configuration.
     */
    @Nested
    @Suppress("unused")
    fun getPluginSettings(): Provider<out ValueHolder<JooqDockerPluginSettings>> = _pluginSettings

    private val migrationRunner = UniversalMigrationRunner(schemas, migrationLocations, flywayProperties)

    private val codegenRunner = UniversalJooqCodegenRunner()

    private val configurationProvider = ConfigurationProvider(
        basePackageName = basePackageName,
        outputDirectory = outputDirectory,
        outputSchemaToDefault = outputSchemaToDefault,
        schemaToPackageMapping = schemaToPackageMapping,
        schemas = schemas,
        logLevel = logger.getCodegenLogging(),
    )

    private fun classLoaders() = CodegenClasspathAwareClassLoaders.from(codegenClasspath)

    init {
        group = "jooq"
        description = "Generates jOOQ classes from Flyway migrations"
    }

    override fun withContainer(configure: Action<WithContainer>) = localPluginSettings.set(
        globalPluginSettings.map { settings ->
            settings.let { it as? WithContainer }?.copy()?.apply(configure::execute) ?: WithContainer(configure)
        }
    )

    override fun withoutContainer(configure: Action<WithoutContainer>) = localPluginSettings.set(
        globalPluginSettings.map { settings ->
            settings.let { it as? WithoutContainer }?.copy()?.apply(configure::execute) ?: WithoutContainer(configure)
        }
    )

    /**
     * Configure the jOOQ code generator with an XML configuration file.
     */
    @Suppress("unused")
    fun usingXmlConfig(
        file: RegularFile = projectLayout.projectDirectory.file("src/main/resources/db/jooq.xml"),
        customizer: Action<Generator> = Action<Generator> { }
    ) {
        _generatorConfig.set(
            configurationProvider.fromXml(providerFactory.fileContents(file)).map { config ->
                config.also { customizer.execute(it.generator) }
            }.map(::PrivateValueHolder)
        )
    }

    /**
     * Configure the jOOQ code generator with an XML configuration file.
     */
    @Suppress("unused")
    fun usingXmlConfig(file: RegularFile, closure: Closure<Generator>) = usingXmlConfig(file, closure::callWith)

    /**
     * Configure the jOOQ code generator programmatically with [Generator].
     */
    @Suppress("unused")
    fun usingJavaConfig(customizer: Action<Generator>) {
        _generatorConfig.set(
            providerFactory.provider {
                configurationProvider.defaultConfiguration().also {
                    customizer.execute(it.generator)
                }
            }.map(::PrivateValueHolder)
        )
    }

    /**
     * Configure the jOOQ code generator programmatically with [Generator].
     */
    @Suppress("unused")
    fun usingJavaConfig(closure: Closure<Generator>) = usingJavaConfig(closure::callWith)

    @TaskAction
    fun generateClasses() {
        _pluginSettings.get().value
            .runWithDatabaseCredentials(classLoaders()) { classLoaders, credentials ->
                val schemaVersion = migrationRunner.migrateDb(classLoaders, credentials)
                generateJooqClasses(classLoaders, credentials, schemaVersion)
            }
    }

    private fun generateJooqClasses(
        jdbcAwareClassLoader: CodegenClasspathAwareClassLoaders,
        credentials: DatabaseCredentials,
        schemaVersion: SchemaVersion
    ) {
        fsOperations.delete {
            delete(outputDirectory)
        }
        codegenRunner.generateJooqClasses(
            codegenAwareClassLoader = jdbcAwareClassLoader,
            configuration = _generatorConfig.get().value.postProcess(
                schemaVersion = schemaVersion,
                credentials = credentials,
                extraTableExclusions = listOfNotNull(
                    migrationRunner.flywayTableName.takeUnless { includeFlywayTable.get() }
                )
            )
        )
    }

    /**
     * Set location of Flyway migrations to use for code generation
     *
     * @see migrationLocations
     * @see MigrationLocation
     */
    fun ListProperty<MigrationLocation>.set(migrationLocation: MigrationLocation) = set(listOf(migrationLocation))

    /**
     * Set location of SQL migrations on the file system to use for code generation
     *
     * Example:
     *
     * ```
     * migrationLocations.setFromFilesystem(project.files("src/main/resources/db/migration"))
     * ```
     *
     * @see migrationLocations
     * @see MigrationLocation
     * @see Project.files
     */
    fun ListProperty<MigrationLocation>.setFromFilesystem(files: FileCollection) = set(
        MigrationLocation.Filesystem(files)
    )

    /**
     * Set location of SQL migrations on the file system to use for code generation
     *
     * Example:
     *
     * ```
     * migrationLocations.setFromFilesystem("src/main/resources/db/migration")
     * ```
     *
     * @see migrationLocations
     * @see MigrationLocation
     */
    fun ListProperty<MigrationLocation>.setFromFilesystem(path: String) = setFromFilesystem(project.files(path))

    /**
     * Add location of Java-based or SQL migrations to Flyway classpath from the specified path
     *
     * Example:
     *
     * ```
     * tasks.generateJooqClasses {
     *     migrationLocations.setFromClasspath(project.files("build/libs/some.jar"))
     * }
     * ```
     *
     * @see migrationLocations
     * @see MigrationLocation
     * @see Project.files
     * @see Project.project
     * @see FileCollection
     */
    fun ListProperty<MigrationLocation>.setFromClasspath(
        path: FileCollection,
        location: String = "/db/migration"
    ) = set(MigrationLocation.Classpath(path, location))

    /**
     * Add location of Java-based or SQL migrations to Flyway classpath from the specified provider
     *
     * Examples:
     *
     * Using compiled Java-based migrations from a Gradle submodule:
     *
     * ```
     * tasks.generateJooqClasses {
     *    migrationLocations.setFromClasspath(
     *       project(":migrations").sourceSets.main.map { it.output }
     *    )
     * }
     * ```
     *
     * Similarly, if you use some extra dependencies in your migrations module, you can do this:
     *
     *
     * ```
     * tasks.generateJooqClasses {
     *    migrationLocations.setFromClasspath(
     *       // notice extra + it.runtimeClasspath
     *       project(":migrations").sourceSets.main.map { it.output + it.runtimeClasspath }
     *    )
     * }
     * ```
     *
     * @see migrationLocations
     * @see MigrationLocation
     * @see Project.project
     */
    fun <T> ListProperty<MigrationLocation>.setFromClasspath(
        pathProvider: Provider<T>,
        location: String = "/db/migration"
    ) = setFromClasspath(project.files(pathProvider), location)

    /**
     * Add location of Java-based or SQL migrations to Flyway classpath from a configuration
     *
     * Example:
     *
     * ```
     * val migrationClasspath by configurations.creating
     *
     * dependencies {
     *    migrationClasspath("third.party:some.artifact:some.version")
     * }
     *
     * tasks.generateJooqClasses {
     *     migrationLocations.setFromClasspath(migrationClasspath)
     * }
     * ```
     *
     * @see migrationLocations
     * @see MigrationLocation
     * @see Project.getConfigurations
     * @see GradleConfiguration
     */
    fun ListProperty<MigrationLocation>.setFromClasspath(
        configuration: GradleConfiguration,
        location: String = "/db/migration"
    ) = setFromClasspath(project.files(configuration), location)
}

private data class PrivateValueHolder<T>(@get:Nested val value: T) : ValueHolder<T>()
