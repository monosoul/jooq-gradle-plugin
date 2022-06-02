package dev.monosoul.jooq

import org.gradle.api.Action
import java.io.Serializable

open class JooqExtension : Serializable {
    val jdbc = Jdbc()
    val db = Database()
    val image = Image(db)

    fun db(configure: Action<Database>) {
        configure.execute(db)
    }

    fun image(configure: Action<Image>) {
        configure.execute(image)
    }

    fun jdbc(configure: Action<Jdbc>) {
        configure.execute(jdbc)
    }

    class Jdbc : Serializable {
        var schema = "jdbc:postgresql"
        var driverClassName = "org.postgresql.Driver"
        var jooqMetaName = "org.jooq.meta.postgres.PostgresDatabase"
        var urlQueryParams = ""
    }

    class Database : Serializable {
        var username = "postgres"
        var password = "postgres"
        var name = "postgres"
        var hostOverride: String? = null
        var port = 5432
    }

    class Image(private val db: Database) : Serializable {
        var repository = "postgres"
        var tag = "11.2-alpine"
        var envVars: Map<String, Any> = mapOf(
            "POSTGRES_USER" to db.username,
            "POSTGRES_PASSWORD" to db.password,
            "POSTGRES_DB" to db.name
        )
        var readinessProbe = "SELECT 1"
        var command: String? = null

        internal fun getImageName(): String {
            return "$repository:$tag"
        }
    }
}

