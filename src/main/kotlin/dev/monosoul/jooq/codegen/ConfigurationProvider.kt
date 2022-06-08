package dev.monosoul.jooq.codegen

import dev.monosoul.jooq.migration.SchemaVersion
import dev.monosoul.jooq.settings.DatabaseCredentials
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.jooq.codegen.GenerationTool
import org.jooq.codegen.JavaGenerator
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.MatcherRule
import org.jooq.meta.jaxb.MatcherTransformType.AS_IS
import org.jooq.meta.jaxb.Matchers
import org.jooq.meta.jaxb.MatchersCatalogType
import org.jooq.meta.jaxb.MatchersSchemaType
import org.jooq.meta.jaxb.SchemaMappingType
import org.jooq.meta.jaxb.Strategy
import org.jooq.meta.jaxb.Target
import java.io.File
import java.io.InputStream

internal class ConfigurationProvider(
    private val basePackageName: Property<String>,
    private val outputDirectory: DirectoryProperty,
    private val outputSchemaToDefault: SetProperty<String>,
    private val schemaToPackageMapping: MapProperty<String, String>,
    private val schemas: ListProperty<String>,
) {

    fun fromXml(file: File) = file.inputStream().use(::load).applyCommonConfiguration()

    fun defaultConfiguration() = Generator()
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

    private fun Configuration.applyCommonConfiguration() = also { config ->
        config.generator.apply {
            withLogging(Logging.DEBUG)
            withTarget(codeGenTarget())
            schemaToPackageMapping.get().toMatchersStrategy()?.also { matchers ->
                withStrategy(
                    Strategy().withMatchers(matchers)
                )
            }
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

    private fun Map<String, String>.toMatchersStrategy() = takeIf { it.isNotEmpty() }?.let {
        Matchers()
            .withSchemas(
                *it.toSchemasMatchers()
            )
            .withCatalogs(
                *it.toCatalogMatchers()
            )
    }

    private fun Map<String, String>.toSchemasMatchers() = map { (schema, pkg) ->
        MatchersSchemaType()
            .withExpression(schema)
            .withSchemaIdentifier(
                MatcherRule()
                    .withTransform(AS_IS)
                    .withExpression(pkg)
            )
    }.toTypedArray()

    private fun Map<String, String>.toCatalogMatchers() = map { (schema, pkg) ->
        MatchersCatalogType()
            .withExpression(schema)
            .withCatalogIdentifier(
                MatcherRule()
                    .withTransform(AS_IS)
                    .withExpression(pkg)
            )
    }.toTypedArray()

    internal companion object {
        private fun load(inputStream: InputStream): Configuration = GenerationTool.load(inputStream)

        private fun Generator.addExcludes(excludes: List<String>) {
            database.withExcludes(
                (excludes + database.excludes).filterNot(String::isBlank).joinToString("|")
            )
        }

        fun Configuration.postProcess(
            schemaVersion: SchemaVersion,
            credentials: DatabaseCredentials,
            extraTableExclusions: List<String> = emptyList(),
        ) = apply {
            generator.also {
                it.addExcludes(extraTableExclusions)
                it.database.schemaVersionProvider = schemaVersion.value
            }
            withJdbc(
                Jdbc()
                    .withDriver(credentials.jdbcDriverClassName)
                    .withUrl(credentials.jdbcUrl)
                    .withUser(credentials.username)
                    .withPassword(credentials.password)
            )
        }
    }
}
