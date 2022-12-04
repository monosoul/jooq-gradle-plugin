import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

plugins {
    `kotlin-dsl`
    `kotlin-convention`
    jacoco
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.jacoco.testkit)
    alias(libs.plugins.shadow)
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
            name = "Snapshot"
            url = uri("https://maven.pkg.github.com/monosoul/jooq-gradle-plugin")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "localBuild"
            url = uri("build/local-repository")
        }
    }
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

    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(false)
        }
        dependsOn(withType<Test>())
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
