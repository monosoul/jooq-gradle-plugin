/**
 * This is how you can make the plugin to use an older version of jOOQ.
 * This way you make sure the plugin configuration DSL uses the same jOOQ version as the one that will be used to
 * generate classes.
 */

plugins {
    id("org.springframework.boot") version "3.0.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    val kotlinVersion = "1.7.22"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("dev.monosoul.jooq-docker") version "5.0.0"
}

buildscript {
    val oldJooqVersion by extra { "3.14.15" }
    configurations.classpath {
        resolutionStrategy {
            setForcedModules("org.jooq:jooq-codegen:$oldJooqVersion")
        }
    }
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
    val oldJooqVersion: String by project.extra
    project.extra["jooq.version"] = oldJooqVersion
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
}
