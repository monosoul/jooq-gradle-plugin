package dev.monosoul.jooq

import dev.monosoul.GenericDatabaseContainer
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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
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
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
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

    @Internal
    fun getDb() = getExtension().db

    @Internal
    fun getJdbc() = getExtension().jdbc

    @Internal
    fun getImage() = getExtension().image

    @Input
    fun getJdbcSchema() = getJdbc().schema

    @Input
    fun getJdbcDriverClassName() = getJdbc().driverClassName

    @Input
    fun getJooqMetaName() = getJdbc().jooqMetaName

    @Input
    fun getJdbcUrlQueryParams() = getJdbc().urlQueryParams

    @Input
    fun getDbUsername() = getDb().username

    @Input
    fun getDbPassword() = getDb().password

    @Input
    fun getDbPort() = getDb().port

    @Input
    @Optional
    fun getDbHostOverride() = getDb().hostOverride

    @Input
    fun getImageRepository() = getImage().repository

    @Input
    fun getImageTag() = getImage().tag

    @Input
    fun getImageEnvVars() = getImage().envVars

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
        val image = getImage()
        val db = getDb()
        val jdbc = getJdbc()
        val jdbcAwareClassLoader = buildJdbcArtifactsAwareClassLoader()

        val dbContainer = GenericDatabaseContainer(
            imageName = image.getImageName(),
            env = image.envVars.mapValues { (_, value) -> value.toString() },
            testQueryString = image.testQuery,
            database = db,
            jdbc = jdbc,
            jdbcAwareClassLoader = jdbcAwareClassLoader,
            command = image.command,
        ).also { it.start() }
        val jdbcUrl = dbContainer.jdbcUrl
        try {
            migrateDb(jdbcAwareClassLoader, jdbcUrl)
            generateJooqClasses(jdbcAwareClassLoader, jdbcUrl)
        } finally {
            dbContainer.stop()
        }
    }

    private fun migrateDb(jdbcAwareClassLoader: ClassLoader, jdbcUrl: String) {
        val db = getDb()
        Flyway.configure(jdbcAwareClassLoader)
            .dataSource(jdbcUrl, db.username, db.password)
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

    private fun generateJooqClasses(jdbcAwareClassLoader: ClassLoader, jdbcUrl: String) {
        project.delete(outputDirectory)
        val db = getDb()
        val jdbc = getJdbc()
        FlywaySchemaVersionProvider.setup(defaultFlywaySchema(), flywayTableName())
        SchemaPackageRenameGeneratorStrategy.schemaToPackageMapping.set(schemaToPackageMapping.toMap())
        val tool = GenerationTool()
        tool.setClassLoader(jdbcAwareClassLoader)
        generatorConfig.get().also {
            excludeFlywaySchemaIfNeeded(it.generator)
        }.apply {
            withJdbc(
                Jdbc()
                    .withDriver(jdbc.driverClassName)
                    .withUrl(jdbcUrl)
                    .withUser(db.username)
                    .withPassword(db.password)
            )
        }.run(tool::run)
    }

    private fun defaultGeneratorConfig() = Generator()
        .withName(JavaGenerator::class.qualifiedName)
        .withDatabase(
            Database()
                .withName(getJdbc().jooqMetaName)
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

    private fun buildJdbcArtifactsAwareClassLoader(): ClassLoader {
        return URLClassLoader(resolveJdbcArtifacts(), project.buildscript.classLoader)
    }

    @Throws(IOException::class)
    private fun resolveJdbcArtifacts(): Array<URL> {
        return project.configurations.getByName("jdbc").resolvedConfiguration.resolvedArtifacts.map {
            it.file.toURI().toURL()
        }.toTypedArray()
    }
}
