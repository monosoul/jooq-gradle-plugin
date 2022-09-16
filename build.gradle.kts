import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    jacoco
    id("com.gradle.plugin-publish") version "1.0.0"
    id("pl.droidsonroids.jacoco.testkit") version "1.0.9"
    `java-test-fixtures`
}

/**
 * disable test fixtures publishing
 * https://docs.gradle.org/current/userguide/java_testing.html#publishing_test_fixtures
 */
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

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

        displayName = "jOOQ Docker Plugin"
        description = "Generates jOOQ classes using dockerized database"
    }
}

pluginBundle {
    website = "https://github.com/monosoul/jooq-gradle-plugin"
    vcsUrl = "https://github.com/monosoul/jooq-gradle-plugin"

    pluginTags = mapOf(
        "jooqDockerPlugin" to listOf("jooq", "docker", "db"),
    )
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

val jooqVersion = "3.17.4"
val flywayVersion = "9.3.0"

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
    val testcontainersVersion = "1.17.3"
    implementation("org.testcontainers:jdbc:$testcontainersVersion")

    testFixturesApi("org.testcontainers:postgresql:$testcontainersVersion")
    testFixturesApi(enforcedPlatform("org.junit:junit-bom:5.9.0"))
    testFixturesApi("org.junit.jupiter:junit-jupiter")
    testFixturesApi("io.strikt:strikt-jvm:0.34.1")
    testFixturesApi("io.mockk:mockk:1.12.8")
    testFixturesApi(gradleTestKit())
}
