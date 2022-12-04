plugins {
    kotlin("jvm")
    `kotlin-convention`
    alias(libs.plugins.shadow) apply false
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom"))

    testImplementation(libs.testcontainers.postgresql)
    testImplementation(enforcedPlatform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.strikt)
    testImplementation("ch.qos.logback:logback-classic:1.4.5")
}

tasks {
    val publishAllPublicationsToLocalBuildRepository: Task by rootProject.tasks

    val copyJar by registering(Copy::class) {
        inputs.files(publishAllPublicationsToLocalBuildRepository, files("${rootProject.buildDir}/local-repository"))
        from(publishAllPublicationsToLocalBuildRepository, files("${rootProject.buildDir}/local-repository"))
        into("$buildDir/local-repository")

        outputs.files("$buildDir/local-repository")
    }

    test {
        inputs.files(copyJar)
    }

    withType<ProcessResources> {
        filesMatching("**/build.gradle.kts") {
            filter {
                it.replace("@plugin.version@", rootProject.version.toString())
            }
        }
    }
}
