plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(files("../plugin/plugin.jar"))
    implementation("net.java.dev.jna:jna:@jna.version@")
    implementation("org.jooq:jooq-codegen:@jooq.version@")
    implementation("org.flywaydb:flyway-core:@flyway.version@")
}
