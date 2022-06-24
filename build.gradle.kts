import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    jacoco
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.21.0"
    id("pl.droidsonroids.jacoco.testkit") version "1.0.9"
}

repositories {
    mavenCentral()
}

val targetJava = JavaVersion.VERSION_1_8
java {
    sourceCompatibility = targetJava
    targetCompatibility = targetJava
}

group = "dev.monosoul.jooq"

gradlePlugin {
    plugins.create("jooqDockerPlugin") {
        id = "dev.monosoul.jooq-docker"
        implementationClass = "dev.monosoul.jooq.JooqDockerPlugin"
        version = project.version
    }
}

pluginBundle {
    website = "https://github.com/monosoul/jooq-gradle-plugin"
    vcsUrl = "https://github.com/monosoul/jooq-gradle-plugin"

    description = "Generates jOOQ classes using dockerized database"

    (plugins) {
        "jooqDockerPlugin" {
            displayName = "jOOQ Docker Plugin"
            tags = listOf("jooq", "docker", "db")
            version = project.version.toString()
        }
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(STARTED, PASSED, FAILED)
            showExceptions = true
            showStackTraces = true
            showCauses = true
            exceptionFormat = FULL
        }
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "$targetJava"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(false)
        }
        setDependsOn(withType<Test>())
    }
}

val jooqVersion = "3.17.0"
val flywayVersion = "8.5.13"

tasks.withType<ProcessResources> {
    filesMatching("**/dev.monosoul.jooq.dependency.versions") {
        filter {
            it.replace("@jooq.version@", jooqVersion)
                .replace("@flyway.version@", flywayVersion)
        }
    }
}

dependencies {
    implementation("org.jooq:jooq-codegen:$jooqVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    val testcontainersVersion = "1.17.2"
    implementation("org.testcontainers:jdbc:$testcontainersVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation(enforcedPlatform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.strikt:strikt-jvm:0.34.1")
    testImplementation("io.mockk:mockk:1.12.4")
    testCompileOnly(gradleTestKit())
}
