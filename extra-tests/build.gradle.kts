/**
 * This module is required because Gradle doesn't support Java agents
 * when using TestKit with configuration cache enabled
 *
 * https://docs.gradle.org/7.5.1/userguide/configuration_cache.html#config_cache:not_yet_implemented:testkit_build_with_java_agent
 */

plugins {
    `kotlin-dsl`
    `kotlin-convention`
}

dependencies {
    implementation(rootProject)
    testImplementation(testFixtures(rootProject))
}

tasks {
    pluginUnderTestMetadata {
        pluginClasspath.from(rootProject.configurations.named("shadow"))
    }
}
