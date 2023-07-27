import dev.monosoul.jooq.RecommendedVersions.FLYWAY_VERSION

plugins {
    kotlin("jvm")
    id("dev.monosoul.jooq-docker") apply false
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.flywaydb:flyway-core:$FLYWAY_VERSION")
}
