import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

    withType<ProcessResources> {
        filesMatching("**/build.gradle.kts") {
            filter {
                it.replace("@jooq.version@", libs.versions.jooq.get())
                    .replace("@flyway.version@",libs.versions.flyway.get())
                    .replace("@jna.version@", libs.versions.jna.get())
            }
        }
    }
}
