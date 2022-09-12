/**
 * This is how you can align the jOOQ version used by Spring Framework with the jOOQ version used by the plugin
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    val kotlinVersion = "1.6.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("dev.monosoul.jooq-docker") version "1.3.8"
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Codegen
    jooqCodegen("org.postgresql:postgresql")

    // Spring Boot
    project.extra["jooq.version"] = RecommendedVersions.JOOQ_VERSION
    project.extra["flyway.version"] = RecommendedVersions.FLYWAY_VERSION
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
}
