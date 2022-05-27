package dev.monosoul.jooq

import org.jooq.codegen.DefaultGeneratorStrategy
import org.jooq.meta.CatalogDefinition
import org.jooq.meta.Definition
import org.jooq.meta.SchemaDefinition

class SchemaPackageRenameGeneratorStrategy : DefaultGeneratorStrategy() {
    companion object {
        val schemaToPackageMapping: ThreadLocal<Map<String, String>> = ThreadLocal.withInitial { emptyMap() }
    }

    override fun getJavaIdentifier(definition: Definition?): String {
        if (isEligibleForRename(definition)) {
            return getMappingForDefinition(definition)!!
        } else {
            return super.getJavaIdentifier(definition)
        }
    }

    private fun isEligibleForRename(definition: Definition?) =
            isSchemaOrCatalog(definition) && getMappingForDefinition(definition) != null

    private fun isSchemaOrCatalog(definition: Definition?) =
            definition is SchemaDefinition || definition is CatalogDefinition

    private fun getMappingForDefinition(definition: Definition?) =
            schemaToPackageMapping.get()[definition?.getSchemaName()]

    private fun Definition.getSchemaName(): String {
        return inputName
    }

}
