package dev.monosoul.jooq

import dev.monosoul.jooq.settings.DatabaseCredentials
import dev.monosoul.jooq.settings.JooqDockerPluginSettings
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import dev.monosoul.jooq.settings.SettingsAware
import dev.monosoul.jooq.util.MigrationRunner
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.jooq.codegen.GenerationTool
import org.jooq.codegen.JavaGenerator
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.SchemaMappingType
import org.jooq.meta.jaxb.Strategy
import org.jooq.meta.jaxb.Target
import java.io.File
import javax.inject.Inject

@CacheableTask
open class GenerateJooqClassesTask @Inject constructor(
    objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory,
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
     * Exclude Flyway migration history table from generated code.
     */
    @Input
    val excludeFlywayTable = objectFactory.property<Boolean>().convention(false)

    /**
     * Code generator configuration.
     *
     * Avoid changing manually, use [usingJavaConfig] or [usingXmlConfig] instead.
     */
    @Input
    val generatorConfig = objectFactory.property<Configuration>().convention(
        providerFactory.provider(::defaultGeneratorConfig)
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
        objectFactory.directoryProperty().convention(project.layout.buildDirectory.dir("generated-jooq"))

    private var localPluginSettings: JooqDockerPluginSettings? = null

    /**
     * Local (task-specific) plugin configuration.
     *
     * Avoid changing manually, use [withContainer] or [withoutContainer] instead.
     */
    @Input
    fun getPluginSettings() = localPluginSettings ?: globalPluginSettings()

    private fun globalPluginSettings() = project.extensions.getByType<JooqExtension>().pluginSettings

    private val migrationRunner by lazy {
        MigrationRunner(schemas, inputDirectory, flywayProperties)
    }

    init {
        group = "jooq"
    }

    override fun withContainer(configure: Action<WithContainer>) {
        localPluginSettings = globalPluginSettings().let { it as? WithContainer }?.copy()?.apply(configure::execute)
            ?: WithContainer(configure)
    }

    override fun withoutContainer(configure: Action<WithoutContainer>) {
        localPluginSettings = globalPluginSettings().let { it as? WithoutContainer }?.copy()?.apply(configure::execute)
            ?: WithoutContainer(configure)
    }

    /**
     * Configure the jOOQ code generator with an XML configuration file.
     */
    @Suppress("unused")
    fun usingXmlConfig(
        file: File = project.file("src/main/resources/db/jooq.xml"),
        customizer: Action<Generator> = Action<Generator> { }
    ) {
        generatorConfig.set(
            providerFactory.provider {
                file.inputStream().use(GenerationTool::load).applyCommonConfiguration().also {
                    it.generator.apply(customizer::execute)
                }
            }
        )
    }

    /**
     * Configure the jOOQ code generator with an XML configuration file.
     */
    @Suppress("unused")
    fun usingXmlConfig(file: File, closure: Closure<Generator>) = usingXmlConfig(file, closure::callWith)

    /**
     * Configure the jOOQ code generator programmatically with [Generator].
     */
    @Suppress("unused")
    fun usingJavaConfig(customizer: Action<Generator>) {
        generatorConfig.set(
            providerFactory.provider {
                defaultGeneratorConfig().also {
                    customizer.execute(it.generator)
                }
            }
        )
    }

    /**
     * Configure the jOOQ code generator programmatically with [Generator].
     */
    @Suppress("unused")
    fun usingJavaConfig(closure: Closure<Generator>) = usingJavaConfig(closure::callWith)

    @TaskAction
    fun generateClasses() {
        getPluginSettings()
            .runWithDatabaseCredentials(project.jdbcAwareClassloaderProvider()) { jdbcAwareClassLoader, credentials ->
                migrationRunner.migrateDb(jdbcAwareClassLoader, credentials)
                generateJooqClasses(jdbcAwareClassLoader, credentials)
            }
    }

    private fun generateJooqClasses(jdbcAwareClassLoader: ClassLoader, credentials: DatabaseCredentials) {
        project.delete(outputDirectory)
        FlywaySchemaVersionProvider.setup(migrationRunner.defaultFlywaySchema(), migrationRunner.flywayTableName())
        SchemaPackageRenameGeneratorStrategy.schemaToPackageMapping.set(schemaToPackageMapping.get())
        val tool = GenerationTool()
        tool.setClassLoader(jdbcAwareClassLoader)
        generatorConfig.get().also {
            excludeFlywaySchemaIfNeeded(it.generator)
        }.apply {
            withJdbc(
                Jdbc()
                    .withDriver(credentials.jdbcDriverClassName)
                    .withUrl(credentials.jdbcUrl)
                    .withUser(credentials.username)
                    .withPassword(credentials.password)
            )
        }.run(tool::run)
    }

    private fun defaultGeneratorConfig() = Generator()
        .withName(JavaGenerator::class.qualifiedName)
        .withDatabase(
            Database()
                .withSchemata(schemas.get().map(this::toSchemaMappingType))
                .withIncludes(".*")
                .withExcludes("")
        )
        .withGenerate(Generate())
        .let {
            Configuration().withGenerator(it)
        }
        .applyCommonConfiguration()

    private fun Configuration.applyCommonConfiguration() = also {
        it.generator.apply {
            withLogging(Logging.DEBUG)
            withTarget(codeGenTarget())
            withStrategy(
                Strategy().withName(SchemaPackageRenameGeneratorStrategy::class.qualifiedName)
            )
            database.withSchemaVersionProvider(FlywaySchemaVersionProvider::class.qualifiedName)
        }
    }

    private fun codeGenTarget() = Target()
        .withPackageName(basePackageName.get())
        .withDirectory(outputDirectory.asFile.get().toString())
        .withEncoding("UTF-8")
        .withClean(true)

    private fun toSchemaMappingType(schemaName: String): SchemaMappingType {
        return SchemaMappingType()
            .withInputSchema(schemaName)
            .withOutputSchemaToDefault(outputSchemaToDefault.get().contains(schemaName))
    }

    private fun excludeFlywaySchemaIfNeeded(generator: Generator) {
        if (excludeFlywayTable.get())
            generator.database.withExcludes(addFlywaySchemaHistoryToExcludes(generator.database.excludes))
    }

    private fun addFlywaySchemaHistoryToExcludes(currentExcludes: String?): String {
        return listOf(currentExcludes, migrationRunner.flywayTableName())
            .filterNot(String?::isNullOrEmpty)
            .joinToString("|")
    }
}
