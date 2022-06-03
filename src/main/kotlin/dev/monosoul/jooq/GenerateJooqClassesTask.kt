package dev.monosoul.jooq

import dev.monosoul.jooq.settings.JdbcDriverClassName
import dev.monosoul.jooq.settings.JdbcUrl
import dev.monosoul.jooq.settings.Password
import dev.monosoul.jooq.settings.Username
import groovy.lang.Closure
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location.FILESYSTEM_PREFIX
import org.flywaydb.core.internal.configuration.ConfigUtils.DEFAULT_SCHEMA
import org.flywaydb.core.internal.configuration.ConfigUtils.TABLE
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
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
    private val objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory,
) : DefaultTask() {
    @Input
    var schemas = arrayOf("public")

    @Input
    var basePackageName = "org.jooq.generated"

    @Input
    var flywayProperties = emptyMap<String, String>()

    @Input
    var outputSchemaToDefault = emptySet<String>()

    @Input
    var schemaToPackageMapping = emptyMap<String, String>()

    @Input
    var excludeFlywayTable = false

    @Input
    val generatorConfig = objectFactory.property<Configuration>().convention(
        providerFactory.provider(::defaultGeneratorConfig)
    )

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    val inputDirectory = objectFactory.fileCollection().from("src/main/resources/db/migration")

    @OutputDirectory
    val outputDirectory =
        objectFactory.directoryProperty().convention(project.layout.buildDirectory.dir("generated-jooq"))

    @Input
    fun getPluginSettings() = getExtension().pluginSettings

    init {
        project.plugins.withType(JavaPlugin::class.java) {
            project.extensions.getByType<JavaPluginExtension>().sourceSets.named(MAIN_SOURCE_SET_NAME) {
                java {
                    srcDir(outputDirectory)
                }
            }
        }
    }

    private fun getExtension() = project.extensions.getByName("jooq") as JooqExtension

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

    @Deprecated(
        message = "Use usingJavaConfig instead",
        replaceWith = ReplaceWith("usingJavaConfig(customizer)"),
    )
    fun customizeGenerator(customizer: Action<Generator>) = usingJavaConfig(customizer)

    @Deprecated(
        message = "Use usingJavaConfig instead",
        replaceWith = ReplaceWith("usingJavaConfig(closure)"),
    )
    fun customizeGenerator(closure: Closure<Generator>) = usingJavaConfig {
        closure.rehydrate(this, this, this).call(this)
    }

    @TaskAction
    fun generateClasses() {
        getPluginSettings().runWithDatabaseCredentials(project.jdbcAwareClassloaderProvider()) { jdbcAwareClassLoader, jdbcDriverClassName, jdbcUrl, username, password ->
            migrateDb(jdbcAwareClassLoader, jdbcUrl, username, password)
            generateJooqClasses(jdbcAwareClassLoader, jdbcDriverClassName, jdbcUrl, username, password)
        }
    }

    private fun migrateDb(jdbcAwareClassLoader: ClassLoader, jdbcUrl: JdbcUrl, username: Username, password: Password) {
        val db = getPluginSettings()
        Flyway.configure(jdbcAwareClassLoader)
            .dataSource(jdbcUrl.value, username.value, password.value)
            .schemas(*schemas)
            .locations(*inputDirectory.map { "$FILESYSTEM_PREFIX${it.absolutePath}" }.toTypedArray())
            .defaultSchema(defaultFlywaySchema())
            .table(flywayTableName())
            .configuration(flywayProperties)
            .load()
            .migrate()
    }

    private fun defaultFlywaySchema() = flywayProperties[DEFAULT_SCHEMA] ?: schemas.first()

    private fun flywayTableName() = flywayProperties[TABLE] ?: "flyway_schema_history"

    private fun generateJooqClasses(
        jdbcAwareClassLoader: ClassLoader,
        driverClassName: JdbcDriverClassName,
        jdbcUrl: JdbcUrl,
        username: Username,
        password: Password
    ) {
        project.delete(outputDirectory)
        FlywaySchemaVersionProvider.setup(defaultFlywaySchema(), flywayTableName())
        SchemaPackageRenameGeneratorStrategy.schemaToPackageMapping.set(schemaToPackageMapping.toMap())
        val tool = GenerationTool()
        tool.setClassLoader(jdbcAwareClassLoader)
        generatorConfig.get().also {
            excludeFlywaySchemaIfNeeded(it.generator)
        }.apply {
            withJdbc(
                Jdbc()
                    .withDriver(driverClassName.value)
                    .withUrl(jdbcUrl.value)
                    .withUser(username.value)
                    .withPassword(password.value)
            )
        }.run(tool::run)
    }

    private fun defaultGeneratorConfig() = Generator()
        .withName(JavaGenerator::class.qualifiedName)
        .withDatabase(
            Database()
                .withSchemata(schemas.map(this::toSchemaMappingType))
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
        .withPackageName(basePackageName)
        .withDirectory(outputDirectory.asFile.get().toString())
        .withEncoding("UTF-8")
        .withClean(true)

    private fun toSchemaMappingType(schemaName: String): SchemaMappingType {
        return SchemaMappingType()
            .withInputSchema(schemaName)
            .withOutputSchemaToDefault(outputSchemaToDefault.contains(schemaName))
    }

    private fun excludeFlywaySchemaIfNeeded(generator: Generator) {
        if (excludeFlywayTable)
            generator.database.withExcludes(addFlywaySchemaHistoryToExcludes(generator.database.excludes))
    }

    private fun addFlywaySchemaHistoryToExcludes(currentExcludes: String?): String {
        return listOf(currentExcludes, flywayTableName())
            .filterNot(String?::isNullOrEmpty)
            .joinToString("|")
    }
}
