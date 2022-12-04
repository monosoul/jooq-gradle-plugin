plugins {
    kotlin("jvm")
    `kotlin-convention`
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom"))

    testImplementation(libs.testcontainers.core)
    testImplementation(enforcedPlatform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.strikt)
}
