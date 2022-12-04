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

    val copyArtifacts by registering(Copy::class) {
        val localRepositoryDirName: String by rootProject.extra

        dependsOn(publishAllPublicationsToLocalBuildRepository)
        inputs.dir(rootProject.layout.buildDirectory.dir(localRepositoryDirName))
        from(rootProject.layout.buildDirectory.dir(localRepositoryDirName))

        into(layout.buildDirectory.dir(localRepositoryDirName))
        outputs.dir(layout.buildDirectory.dir(localRepositoryDirName))
    }

    test {
        inputs.files(copyArtifacts)
    }

    withType<ProcessResources> {
        filesMatching("**/build.gradle.kts") {
            filter {
                it.replace("@plugin.version@", rootProject.version.toString())
            }
        }
    }
}
