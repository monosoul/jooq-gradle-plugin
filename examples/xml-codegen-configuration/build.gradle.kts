/**
 * This is how you can configure jOOQ's code generation with XML config
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.8.10"
    id("dev.monosoul.jooq-docker") version "6.0.0"
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
        usingXmlConfig(project.layout.projectDirectory.file("src/main/resources/db/jooq.xml")) {
            name = "org.jooq.codegen.KotlinGenerator"
        }
    }
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.5.4")
    implementation("org.postgresql:postgresql:42.5.4")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
