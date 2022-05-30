import dev.monosoul.jooq.SchemaPackageRenameGeneratorStrategy
import org.jooq.meta.CatalogDefinition
import org.jooq.meta.Database
import org.jooq.meta.SchemaDefinition
import org.jooq.meta.postgres.PostgresTableDefinition
import spock.lang.Specification

class SchemaPackageRenameGeneratorStrategySpec extends Specification {

    def underSpec = new SchemaPackageRenameGeneratorStrategy()

    def "returns name from mapping when schema or catalog encountered"() {
        given:
            SchemaPackageRenameGeneratorStrategy.schemaToPackageMapping.set(["other": "newName"])

        when:
            def result = underSpec.getJavaIdentifier(definition)

        then:
            result == expectedResult

        where:
            definition                                                                                  || expectedResult
            new SchemaDefinition(Mock(Database), "other", "")                                           || "newName"
            new SchemaDefinition(Mock(Database), "some", "")                                            || "DEFAULT_SCHEMA"
            new CatalogDefinition(Mock(Database), "other", "")                                          || "newName"
            new CatalogDefinition(Mock(Database), "some", "")                                           || "DEFAULT_CATALOG"
            new PostgresTableDefinition(new SchemaDefinition(Mock(Database), "", ""), "table_name", "") || "TABLE_NAME"
    }
}
