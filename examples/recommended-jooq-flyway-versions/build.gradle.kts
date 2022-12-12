/**
 * This is how you can configure jOOQ and Flyway dependency versions aligned with the plugin's built-in versions
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.7.22"
    id("dev.monosoul.jooq-docker") version "3.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.3.6")
    implementation("org.postgresql:postgresql:42.3.6")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
    implementation("org.flywaydb:flyway-core:${RecommendedVersions.FLYWAY_VERSION}")
}
