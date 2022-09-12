/**
 * This is how you can configure jOOQ's code generation with Java config
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.6.21"
    id("dev.monosoul.jooq-docker") version "1.3.8"
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
    jooqCodegen("org.postgresql:postgresql:42.3.6")
    implementation("org.postgresql:postgresql:42.3.6")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
