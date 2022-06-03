package dev.monosoul.jooq

import dev.monosoul.jooq.JooqDockerPluginSettings.Database
import dev.monosoul.jooq.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.JooqDockerPluginSettings.WithoutContainer
import org.gradle.api.Action
import java.io.Serializable

open class JooqExtension : Serializable {
    internal var pluginSettings: JooqDockerPluginSettings = WithContainer.new()

    fun withContainer(configure: Action<WithContainer>) {
        pluginSettings = WithContainer.new {
            configure.execute(this)
        }
    }

    fun withoutContainer(configure: Action<WithoutContainer>) {
        pluginSettings = WithoutContainer.new {
            configure.execute(this)
        }
    }

    class Jdbc : Serializable {
        var schema = "jdbc:postgresql"
        var driverClassName = "org.postgresql.Driver"
        var urlQueryParams = ""
    }

    class Image(db: Database.Internal) : Serializable {
        var repository = "postgres"
        var tag = "11.2-alpine"
        var envVars: Map<String, Any> = mapOf(
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
}

