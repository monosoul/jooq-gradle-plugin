package dev.monosoul.jooq

import dev.monosoul.jooq.SchemaPackageRenameGeneratorStrategy.Companion.schemaToPackageMapping
import io.mockk.mockk
import org.jooq.meta.CatalogDefinition
import org.jooq.meta.SchemaDefinition
import org.jooq.meta.postgres.PostgresTableDefinition
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.streams.asStream


class SchemaPackageRenameGeneratorStrategyTest {

    private val strategy = SchemaPackageRenameGeneratorStrategy().apply {
        schemaToPackageMapping.set(mapOf("other" to "newName"))
    }

    @TestFactory
    fun `should return name from mapping when schema or catalog encountered`() = sequenceOf(
        SchemaDefinition(mockk(), "other", "") to "newName",
        SchemaDefinition(mockk(relaxed = true), "some", "") to "DEFAULT_SCHEMA",
        CatalogDefinition(mockk(), "other", "") to "newName",
        CatalogDefinition(mockk(relaxed = true), "some", "") to "DEFAULT_CATALOG",
        PostgresTableDefinition(SchemaDefinition(mockk(), "", ""), "table_name", "") to "TABLE_NAME",
    ).map { (definition, expected) ->
        dynamicTest("given ${definition.name} should return $expected") {
            expectThat(strategy.getJavaIdentifier(definition)) isEqualTo expected
        }
    }.asStream()
}
