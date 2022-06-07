package dev.monosoul.jooq.migration

import dev.monosoul.jooq.settings.DatabaseCredentials
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.output.MigrateResult
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction4
import kotlin.reflect.jvm.jvmName

internal class ReflectiveMigrationRunner(
    codegenAwareClassLoader: ClassLoader,
    private val schemas: Array<String>,
    private val migrationLocations: Array<String>,
    private val flywayProperties: Map<String, String>,
    private val credentials: DatabaseCredentials,
    private val defaultFlywaySchema: String,
    private val flywayTable: String,
) : MigrationRunner {

    private val flyway = ReflectiveFlywayConfiguration(codegenAwareClassLoader)

    override fun migrateDb() = flyway
        .dataSource(credentials)
        .schemas(*schemas)
        .locations(*migrationLocations)
        .defaultSchema(defaultFlywaySchema)
        .table(flywayTable)
        .configuration(flywayProperties)
        .load()
        .migrate()

    private class ReflectiveFlywayConfiguration(private val codegenAwareClassLoader: ClassLoader) {
        private val flywayClass = codegenAwareClassLoader.loadClass(Flyway::class.jvmName)
        private val configurationClass = codegenAwareClassLoader.loadClass(FluentConfiguration::class.jvmName)

        private val configureMethod =
            flywayClass.getMethod(oneArgFunctionName(Flyway::configure), ClassLoader::class.java)
        private val configuration = configureMethod.invoke(null, codegenAwareClassLoader)

        fun dataSource(credentials: DatabaseCredentials) = also {
            val dataSourceMethod = configurationClass.getMethod(
                fourArgFunctionName(FluentConfiguration::dataSource),
                String::class.java,
                String::class.java,
                String::class.java
            )
            dataSourceMethod.invoke(configuration, credentials.jdbcUrl, credentials.username, credentials.password)
        }

        fun schemas(vararg schemas: String) = also {
            val schemasMethod = configurationClass.getMethod(
                twoArgFunctionName(FluentConfiguration::schemas),
                Array<String>::class.java
            )
            schemasMethod.invoke(configuration, schemas)
        }

        fun locations(vararg locations: String) = also {
            val locationsMethod = configurationClass.getMethod(
                twoArgFunctionName<FluentConfiguration, Array<String>, FluentConfiguration>(
                    FluentConfiguration::locations
                ),
                Array<String>::class.java
            )
            locationsMethod.invoke(configuration, locations)
        }

        fun defaultSchema(schema: String) = also {
            val defaultSchemaMethod = configurationClass.getMethod(
                twoArgFunctionName(FluentConfiguration::defaultSchema),
                String::class.java
            )
            defaultSchemaMethod.invoke(configuration, schema)
        }

        fun table(table: String) = also {
            val tableMethod = configurationClass.getMethod(
                twoArgFunctionName(FluentConfiguration::table),
                String::class.java
            )
            tableMethod.invoke(configuration, table)
        }

        fun configuration(props: Map<String, String>) = also {
            val configurationMethod = configurationClass.getMethod(
                twoArgFunctionName<FluentConfiguration, Map<String, String>, FluentConfiguration>(
                    FluentConfiguration::configuration
                ),
                Map::class.java
            )
            configurationMethod.invoke(configuration, props)
        }

        fun load(): ReflectiveFlyway {
            val loadMethod = configurationClass.getMethod(FluentConfiguration::load.name)

            return ReflectiveFlyway(
                flywayClass = flywayClass,
                migrateResultClass = codegenAwareClassLoader.loadClass(MigrateResult::class.jvmName),
                flywayInstance = loadMethod.invoke(configuration)
            )
        }

        private companion object {
            fun <P, R> oneArgFunctionName(ref: KFunction1<P, R>) = ref.name
            fun <T, P, R> twoArgFunctionName(ref: KFunction2<T, P, R>) = ref.name
            fun <T, P1, P2, P3, R> fourArgFunctionName(ref: KFunction4<T, P1, P2, P3, R>) = ref.name
        }
    }

    private class ReflectiveFlyway(
        private val flywayClass: Class<*>,
        private val migrateResultClass: Class<*>,
        private val flywayInstance: Any,
    ) {
        fun migrate(): SchemaVersion {
            val migrateMethod = flywayClass.getMethod(Flyway::migrate.name)
            val migrateResult = migrateMethod.invoke(flywayInstance)

            val targetSchemaVersionProperty = migrateResultClass.getField(MigrateResult::targetSchemaVersion.name)
            val targetSchemaVersion = targetSchemaVersionProperty.get(migrateResult) as String

            return SchemaVersion(targetSchemaVersion)
        }
    }
}
