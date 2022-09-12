/**
 * This is how you can make the plugin to use a newer version of jOOQ.
 */

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

    project.extra["jooq.version"] = "3.16.6"
    // Codegen
    jooqCodegen("org.postgresql:postgresql")
    jooqCodegen("org.jooq:jooq-codegen")
    jooqCodegen("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0") // Spring tries to enforce an old version of this library

    // Spring Boot
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
}
