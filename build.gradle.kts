import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    jacoco
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.jacoco.testkit)
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

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/monosoul/jooq-gradle-plugin")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
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

val processTemplates by tasks.registering(Copy::class) {
    from("src/template/kotlin")
    into("build/filtered-templates")

    filter {
        it.replace("@jooq.version@", libs.versions.jooq.get())
            .replace("@flyway.version@", libs.versions.flyway.get())
    }
}

sourceSets.main {
    java {
        srcDir(processTemplates)
    }
}

dependencies {
    implementation(libs.jooq.codegen)

    implementation(libs.flyway.core)
    implementation(libs.testcontainers.jdbc)

    testFixturesApi(libs.testcontainers.postgresql)
    testFixturesApi(enforcedPlatform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter)
    testFixturesApi(libs.strikt)
    testFixturesApi(libs.mockk)
    testFixturesApi(gradleTestKit())
}
