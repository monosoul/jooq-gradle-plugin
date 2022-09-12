/**
 * This is how you can configure the plugin using gradle.properties file or by passing gradle properties via cmd line
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
    }
}

dependencies {
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
    jooqCodegen("mysql:mysql-connector-java:8.0.29")
    jooqCodegen("org.flywaydb:flyway-mysql:${RecommendedVersions.FLYWAY_VERSION}")
}
