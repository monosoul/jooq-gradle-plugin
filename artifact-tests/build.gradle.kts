plugins {
    `kotlin-convention`
    `linter-convention`
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom"))

    testImplementation(libs.testcontainers.postgresql)
    testImplementation(enforcedPlatform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.strikt)
    testImplementation("ch.qos.logback:logback-classic:1.5.3")
}

tasks {
    val publishAllPublicationsToLocalBuildRepository: Task by rootProject.tasks

    val copyArtifacts by registering(Copy::class) {
        val localRepositoryDirName: String by rootProject.extra

        dependsOn(publishAllPublicationsToLocalBuildRepository)

        from(rootProject.layout.buildDirectory.dir(localRepositoryDirName))
        into(layout.buildDirectory.dir(localRepositoryDirName))
    }

    testClasses {
        dependsOn(copyArtifacts)
    }

    test {
        inputs.files(copyArtifacts)
    }

    withType<ProcessResources> {
        filesMatching("**/build.gradle.kts") {
            filter {
                it.replace("@plugin.version@", rootProject.version.toString())
                    .replace("@testcontainers.version@", libs.versions.testcontainers.get())
            }
        }
    }

    processTestTemplates {
        filter {
            it.replace("@gradle.version@", gradle.gradleVersion)
                .replace("@plugin.version@", rootProject.version.toString())
        }
    }
}
