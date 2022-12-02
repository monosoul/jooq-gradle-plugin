import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

plugins {
    `kotlin-dsl`
    jacoco
    id("com.gradle.plugin-publish") version "1.1.0"
    id("pl.droidsonroids.jacoco.testkit") version "1.0.9"
    id("com.github.johnrengelman.shadow") version "7.1.2"
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
    val relocateShadowJar by registering(ConfigureShadowRelocation::class) {
        target = shadowJar.get()
        prefix = "${project.group}.shadow"
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()

        fun inMetaInf(vararg patterns: String) = patterns.map { "META-INF/$it" }.toTypedArray()

        exclude(
            *inMetaInf("maven/**", "NOTICE*", "README*", "CHANGELOG*", "DEPENDENCIES*", "LICENSE*", "ABOUT*"),
            "LICENSE*",
        )

        // workaround to separate shadowed testcontainers configuration
        relocate("docker.client.strategy", "${project.group}.docker.client.strategy")
        relocate(
            "TESTCONTAINERS_DOCKER_CLIENT_STRATEGY",
            "${project.group.toString().toUpperCaseAsciiOnly().replace(".", "_")}_TESTCONTAINERS_DOCKER_CLIENT_STRATEGY"
        )

        dependsOn(relocateShadowJar)
    }

    assemble {
        dependsOn(shadowJar)
    }

    pluginUnderTestMetadata {
        pluginClasspath.from(configurations.shadow) // provides complete plugin classpath to the Gradle testkit
    }

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

val jooqVersion = "3.17.5"
val flywayVersion = "9.8.3"

tasks.withType<ProcessResources> {
    filesMatching("**/dev.monosoul.jooq.dependency.versions") {
        filter {
            it.replace("@jooq.version@", jooqVersion)
                .replace("@flyway.version@", flywayVersion)
        }
    }
}

dependencies {
    shadow("org.jooq:jooq-codegen:$jooqVersion")
    shadow("org.flywaydb:flyway-core:$flywayVersion")

    val testcontainersVersion = "1.17.6"
    implementation("org.testcontainers:jdbc:$testcontainersVersion") {
        exclude(group = "net.java.dev.jna") // cannot be shadowed
        exclude(group = "org.slf4j") // provided by Gradle
    }
    shadow("net.java.dev.jna:jna:5.8.0")

    testFixturesApi("org.testcontainers:postgresql:$testcontainersVersion")
    testFixturesApi(enforcedPlatform("org.junit:junit-bom:5.9.1"))
    testFixturesApi("org.junit.jupiter:junit-jupiter")
    testFixturesApi("io.strikt:strikt-jvm:0.34.1")
    testFixturesApi("io.mockk:mockk-jvm:1.13.2")
    testFixturesApi(gradleTestKit())
}
