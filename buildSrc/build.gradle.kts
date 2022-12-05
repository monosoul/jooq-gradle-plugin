plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation(libs.gradle.plugin.publish)
    implementation(libs.shadow)
    implementation(libs.jacoco.testkit)
}

