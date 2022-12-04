import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.7.22"
    id("dev.monosoul.jooq-docker") version "@plugin.version@"
}

repositories {
    mavenCentral()
}

// simulate testcontainers dependency added by another plugin
buildscript {
    dependencies {
        classpath("org.testcontainers:testcontainers:@testcontainers.version@")
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jooq:jooq-kotlin:${RecommendedVersions.JOOQ_VERSION}")

    jooqCodegen("org.postgresql:postgresql:42.3.6")
}
