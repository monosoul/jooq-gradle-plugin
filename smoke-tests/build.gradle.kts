import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    `kotlin-convention`
    alias(libs.plugins.shadow) apply false
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom"))

    testImplementation(libs.testcontainers.core)
    testImplementation(enforcedPlatform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.strikt)
}

tasks {
    val shadowJar: ShadowJar by rootProject.tasks

    val copyJar by registering(Copy::class) {
        inputs.files(shadowJar)
        from(shadowJar)
        into("$buildDir/libs")

        val newJarName = "plugin.jar"
        rename("${rootProject.name}(\\.jar|\\-\\d+\\.\\d+\\.\\d+\\.jar)", newJarName)
        outputs.files("$buildDir/libs/$newJarName")
    }

    test {
        inputs.files(copyJar)
    }
}
