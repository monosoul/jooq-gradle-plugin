/**
 * This is how you can configure the plugin to use Java-based or SQL migrations
 * from a JAR file provided by third party.
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "2.1.0"
    id("dev.monosoul.jooq-docker") version "6.1.14"
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
    // the line below could be actual maven repo coordinates instead
    migrationClasspath(files("some-maven-repo/migrations-0.0.1.jar"))
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
