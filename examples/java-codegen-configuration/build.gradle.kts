/**
 * This is how you can configure jOOQ's code generation with Java config
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "2.1.0"
    id("dev.monosoul.jooq-docker") version "6.1.14"
}

repositories {
    mavenCentral()
}

tasks {
    generateJooqClasses {
        schemas.set(listOf("public", "other"))
        usingJavaConfig {
            database.withExcludes("BAR")
        }
    }
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.5.4")
    implementation("org.postgresql:postgresql:42.5.4")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
