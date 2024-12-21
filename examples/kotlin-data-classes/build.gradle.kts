/**
 * This is how you can configure jOOQ to generate Kotlin data classes
 */

import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "2.1.0"
    id("dev.monosoul.jooq-docker") version "6.1.14"
}

repositories {
    mavenCentral()
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.5.4")
    implementation("org.postgresql:postgresql:42.5.4")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}

tasks.generateJooqClasses {
    usingJavaConfig {
        withName("org.jooq.codegen.KotlinGenerator")
        generate.apply {
            withPojosAsKotlinDataClasses(true)
            withKotlinNotNullRecordAttributes(true)
            withKotlinNotNullPojoAttributes(true)
            withKotlinNotNullInterfaceAttributes(true)
        }
    }
}