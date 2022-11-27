import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    jacoco
    id("com.gradle.plugin-publish") version "1.1.0"
    id("pl.droidsonroids.jacoco.testkit") version "1.0.9"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `java-test-fixtures`
}

configurations {
    implementation {
        extendsFrom(shadow.get())
    }
}

afterEvaluate {
    with(configurations.shadow.get()) {
        dependencies.remove(project.dependencies.gradleApi())
    }
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

val shadowJar by tasks.getting(ShadowJar::class) {
    archiveClassifier.set("")

    configurations = listOf(project.configurations.shadow.get())

    exclude(
        "migrations/*",
        "META-INF/INDEX.LIST",
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/NOTICE*",
        "META-INF/README*",
        "META-INF/CHANGELOG*",
        "META-INF/DEPENDENCIES*",
        "module-info.class")

    mergeServiceFiles()
}

val relocateShadowJar by tasks.creating(ConfigureShadowRelocation::class) {
    target = shadowJar
    prefix = "dev.monosoul.jooq.shadow"
}

shadowJar.dependsOn(relocateShadowJar)

val jar by tasks.getting(Jar::class) {
    dependsOn(shadowJar)
}

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
    shadow("org.testcontainers:jdbc:$testcontainersVersion")

    testFixturesApi("org.testcontainers:postgresql:$testcontainersVersion")
    testFixturesApi(enforcedPlatform("org.junit:junit-bom:5.9.1"))
    testFixturesApi("org.junit.jupiter:junit-jupiter")
    testFixturesApi("io.strikt:strikt-jvm:0.34.1")
    testFixturesApi("io.mockk:mockk-jvm:1.13.2")
    testFixturesApi(gradleTestKit())
}
