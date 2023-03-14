/**
 * This is how you can generate jOOQ classes using an external database instance. It could be a remote database,
 * or a database container from the docker-compose file.
 * This example uses docker-compose file.
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.8.10"
    id("dev.monosoul.jooq-docker") version "3.0.12"
    id("com.avast.gradle.docker-compose") version "0.16.11"
}

repositories {
    mavenCentral()
}

dockerCompose {
    useComposeFiles.set(
        listOf(
            layout.projectDirectory.file("docker-compose.yml").asFile.absolutePath
        )
    )
    captureContainersOutput.set(false)
    isRequiredBy(tasks.generateJooqClasses)
}

jooq {
    withoutContainer {
        db {
            username = "postgres"
            password = "postgres"
            name = "postgres"
            host = "localhost"
            port = 62345
        }
    }
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.5.4")
    jooqCodegen("org.flywaydb:flyway-mysql:${RecommendedVersions.FLYWAY_VERSION}")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
