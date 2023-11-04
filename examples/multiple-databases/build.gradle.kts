/**
 * This is how you can configure the plugin to generate jOOQ classes for multiple databases
 */

import dev.monosoul.jooq.GenerateJooqClassesTask
import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.9.20"
    id("dev.monosoul.jooq-docker") version "6.0.0"
}

repositories {
    mavenCentral()
}

tasks {
    generateJooqClasses {
        basePackageName.set("org.jooq.generated.postgres")
        migrationLocations.setFromFilesystem("src/main/resources/postgres/migration")
        outputDirectory.set(project.layout.buildDirectory.dir("postgres"))
    }

    register<GenerateJooqClassesTask>("generateJooqClassesForMySql") {
        basePackageName.set("org.jooq.generated.mysql")
        migrationLocations.setFromFilesystem("src/main/resources/mysql/migration")
        outputDirectory.set(project.layout.buildDirectory.dir("mysql"))
        includeFlywayTable.set(true)

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
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.5.4")
    jooqCodegen("mysql:mysql-connector-java:8.0.29")
    jooqCodegen("org.flywaydb:flyway-mysql:${RecommendedVersions.FLYWAY_VERSION}")
    jooqCodegen("org.flywaydb:flyway-database-postgresql:${RecommendedVersions.FLYWAY_VERSION}")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
