/**
 * This is how you can configure the plugin to use Java-based migrations from a submodule with extra dependencies.
 *
 * This is a preferred method of setting it up when your migrations are located in the same project,
 * because this way Gradle cache will work properly for generateJooqClasses task.
 */

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
        basePackageName.set("org.jooq.generated")
        migrationLocations.setFromClasspath(
            project(":submodule-dependency-configuration:migrations")
                .sourceSets.main.map { it.output + it.runtimeClasspath }
        )
    }
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.5.4")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
