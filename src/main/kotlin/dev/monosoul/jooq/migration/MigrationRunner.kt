package dev.monosoul.jooq.migration

internal interface MigrationRunner {
    fun migrateDb(): SchemaVersion
}

internal data class SchemaVersion(val value: String)
