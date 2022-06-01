package dev.monosoul.jooq

import org.gradle.api.Action
import java.io.Serializable
import java.net.ServerSocket

open class JooqExtension(projectName: String) : Serializable {
    val jdbc = Jdbc()
    val db = Database(jdbc)
    val image = Image(db, projectName)

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

    class Database(private val jdbc: Jdbc) : Serializable {
        var username = "postgres"
        var password = "postgres"
        var name = "postgres"
        var hostOverride: String? = null
        var port = 5432
        var exposedPort = lookupFreePort()

        internal fun getUrl(host: String): String {
            return "${jdbc.schema}://$host:$exposedPort/$name${jdbc.urlQueryParams}"
        }

        private fun lookupFreePort(): Int {
            ServerSocket(0).use {
                return it.localPort
            }
        }
    }

    class Image(private val db: Database, projectName: String) : Serializable {
        var repository = "postgres"
        var tag = "11.2-alpine"
        var envVars: Map<String, Any> = mapOf(
            "POSTGRES_USER" to db.username,
            "POSTGRES_PASSWORD" to db.password,
            "POSTGRES_DB" to db.name
        )
        var containerName = "jooq-docker-container-${projectName}"
        var readinessProbeHost = "127.0.0.1"
        var readinessProbe = { host: String, port: Int ->
            arrayOf("sh", "-c", "until pg_isready -h $host -p $port; do echo waiting for db; sleep 1; done;")
        }

        internal fun getReadinessCommand(): Array<String> {
            return readinessProbe(readinessProbeHost, db.port)
        }

        internal fun getImageName(): String {
            return "$repository:$tag"
        }
    }
}

