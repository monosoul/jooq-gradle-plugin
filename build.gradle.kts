plugins {
    `kotlin-dsl`
    `kotlin-convention`
    `publishing-convention`
    `coverage-convention`
    `shadow-convention`
    `java-test-fixtures`
}

/**
 * disable test fixtures publishing
 * https://docs.gradle.org/current/userguide/java_testing.html#publishing_test_fixtures
 */
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

group = "dev.monosoul.jooq"

tasks {
    shadowJar {
        archiveClassifier.set("")
        // workaround to separate shadowed testcontainers configuration
        relocate("docker.client.strategy", "${project.group}.docker.client.strategy")
        relocate(
            "TESTCONTAINERS_DOCKER_CLIENT_STRATEGY",
            "${project.group.toString().toUpperCase().replace(".", "_")}_TESTCONTAINERS_DOCKER_CLIENT_STRATEGY"
        )
    }

    assemble {
        dependsOn(shadowJar)
    }

    pluginUnderTestMetadata {
        pluginClasspath.from(configurations.shadow) // provides complete plugin classpath to the Gradle testkit
    }

    processTemplates {
        filter {
            it.replace("@jooq.version@", libs.versions.jooq.get())
                .replace("@flyway.version@", libs.versions.flyway.get())
        }
    }
}

dependencies {
    /**
     * This is counter-intuitive, but dependencies in implementation or api configuration will actually
     * be shadowed, while dependencies in shadow configuration will be skipped from shadowing and just added as
     * transitive. This is a quirk of the shadow plugin.
     */
    shadow(libs.jooq.codegen)
    shadow(libs.flyway.core)

    implementation(libs.testcontainers.jdbc) {
        exclude(group = libs.jna.get().group) // cannot be shadowed
        exclude(group = "org.slf4j") // provided by Gradle
    }
    shadow(libs.jna)

    testFixturesApi(libs.testcontainers.postgresql)
    testFixturesApi(enforcedPlatform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter)
    testFixturesApi(libs.strikt)
    testFixturesApi(libs.mockk)
    testFixturesApi(gradleTestKit())
}
