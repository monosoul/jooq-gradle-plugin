/**
 * This is how you can configure the plugin to generate jOOQ classes for a database other than PostgreSQL.
 * For example for MySQL.
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.6.21"
    id("dev.monosoul.jooq-docker") version "1.3.8"
}

repositories {
    mavenCentral()
}

jooq {
    withContainer {
        image {
            name = "mysql:8.0.29"
            envVars = mapOf(
                "MYSQL_ROOT_PASSWORD" to "mysql",
                "MYSQL_DATABASE" to "mysql"
            )
        }

        db {
            username = "root"
            password = "mysql"
            name = "mysql"
            port = 3306

            jdbc {
                schema = "jdbc:mysql"
                driverClassName = "com.mysql.cj.jdbc.Driver"
            }
        }
    }
}

dependencies {
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
    jooqCodegen("mysql:mysql-connector-java:8.0.29")
    jooqCodegen("org.flywaydb:flyway-mysql:${RecommendedVersions.FLYWAY_VERSION}")
}
