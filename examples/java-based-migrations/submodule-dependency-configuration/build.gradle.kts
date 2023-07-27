/**
 * This is how you can configure the plugin to use Java-based migrations from a submodule.
 *
 * While this method allows you to pull in some extra dependencies into the Flyway classpath along with your migrations,
 * it doesn't work well with Gradle cache. The reason for that is that the codegen task depends now on JAR artifact of
 * migrations submodule, and Gradle task to build JAR doesn't cache its outputs.
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.8.10"
    id("dev.monosoul.jooq-docker") version "5.0.0"
}

repositories {
    mavenCentral()
}

val migrationClasspath by configurations.creating

tasks {
    generateJooqClasses {
        basePackageName.set("org.jooq.generated")
        migrationLocations.setFromClasspath(migrationClasspath)
    }
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.5.4")
    migrationClasspath(project(":submodule-dependency-configuration:migrations"))
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
