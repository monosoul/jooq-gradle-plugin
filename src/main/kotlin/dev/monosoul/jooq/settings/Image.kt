package dev.monosoul.jooq.settings

import java.io.Serializable

class Image(db: Database.Internal) : Serializable {
    var repository = "postgres"
    var tag = "11.2-alpine"
    var envVars: Map<String, String> = mapOf(
        "POSTGRES_USER" to db.username,
        "POSTGRES_PASSWORD" to db.password,
        "POSTGRES_DB" to db.name
    )
    var testQuery = "SELECT 1"
    var command: String? = null

    internal fun getImageName(): String {
        return "$repository:$tag"
    }
}
