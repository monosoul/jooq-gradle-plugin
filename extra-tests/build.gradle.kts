import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * This module is required because Gradle doesn't support Java agents
 * when using TestKit with configuration cache enabled
 *
 * https://docs.gradle.org/7.5.1/userguide/configuration_cache.html#config_cache:not_yet_implemented:testkit_build_with_java_agent
 */

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(rootProject)
    testImplementation(testFixtures(rootProject))
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
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    pluginUnderTestMetadata {
        pluginClasspath.from(rootProject.configurations.named("shadow"))
    }
}
