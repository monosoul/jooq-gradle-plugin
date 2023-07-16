package dev.monosoul.jooq

import dev.monosoul.jooq.codegen.ConfigurationProvider
import dev.monosoul.jooq.codegen.ConfigurationProvider.Companion.postProcess
import dev.monosoul.jooq.codegen.UniversalJooqCodegenRunner
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
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Generator
import javax.inject.Inject

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

    /**
     * Use [usingJavaConfig] or [usingXmlConfig] to provide configuration.
     */
    @Input
    val generatorConfig = objectFactory.property<ValueHolder<Configuration>>().convention(
        providerFactory.provider {
            configurationProvider.defaultConfiguration()
        }.map(::PrivateValueHolder)
    )

    /**
     * Location of Flyway migrations to use for code generation.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    val inputDirectory = objectFactory.fileCollection().from("src/main/resources/db/migration")

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

    /**
     * Use [withContainer] or [withoutContainer] to provide configuration.
     */
    @Nested
    fun getPluginSettings(): Provider<ValueHolder<JooqDockerPluginSettings>> =
        localPluginSettings.orElse(globalPluginSettings).map(::PrivateValueHolder)

    private val migrationRunner = UniversalMigrationRunner(schemas, inputDirectory, flywayProperties)

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
        generatorConfig.set(
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
        generatorConfig.set(
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
        getPluginSettings().get().value
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
            configuration = generatorConfig.get().value.postProcess(
                schemaVersion = schemaVersion,
                credentials = credentials,
                extraTableExclusions = listOfNotNull(
                    migrationRunner.flywayTableName.takeUnless { includeFlywayTable.get() }
                )
            )
        )
    }
}

private data class PrivateValueHolder<T>(@get:Input val value: T) : ValueHolder<T>()

private val <T> ValueHolder<T>.value: T get() = (this as PrivateValueHolder<T>).value
