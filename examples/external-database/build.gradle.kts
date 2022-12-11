/**
 * This is how you can generate jOOQ classes using an external database instance. It could be a remote database,
 * or a database container from the docker-compose file.
 * This example uses docker-compose file.
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.7.22"
    id("dev.monosoul.jooq-docker") version "2.0.1"
    id("com.avast.gradle.docker-compose") version "0.16.4"
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
    jooqCodegen("org.postgresql:postgresql:42.3.6")
    jooqCodegen("org.flywaydb:flyway-mysql:${RecommendedVersions.FLYWAY_VERSION}")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
