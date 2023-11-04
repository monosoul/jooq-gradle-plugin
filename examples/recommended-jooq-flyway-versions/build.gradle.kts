/**
 * This is how you can configure jOOQ and Flyway dependency versions aligned with the plugin's built-in versions
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.9.20"
    id("dev.monosoul.jooq-docker") version "6.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.5.4")
    implementation("org.postgresql:postgresql:42.5.4")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
    implementation("org.flywaydb:flyway-core:${RecommendedVersions.FLYWAY_VERSION}")
}
