/**
 * This is how you can configure the plugin to use Java-based migrations from a submodule.
 *
 * This is a preferred method of setting it up when your migrations are located in teh same project,
 * because this way Gradle cache will work properly for generateJooqClasses task.
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.8.10"
    id("dev.monosoul.jooq-docker") version "5.0.0"
}

repositories {
    mavenCentral()
}

tasks {
    generateJooqClasses {
        basePackageName.set("org.jooq.generated")
        migrationLocations.setFromClasspath(
            project(":submodule-classes:migrations").sourceSets.main.map { it.output }
        )
    }
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.5.4")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
