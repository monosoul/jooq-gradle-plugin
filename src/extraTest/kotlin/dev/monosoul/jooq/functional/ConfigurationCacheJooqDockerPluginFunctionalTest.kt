package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.java.exists
import strikt.java.notExists
import java.io.File

class ConfigurationCacheJooqDockerPluginFunctionalTest : FunctionalTestBase() {
    @TempDir
    private lateinit var localBuildCacheDirectory: File

    @Test
    fun `should work with configuration cache enabled`() {
        // given
        configureLocalGradleCache()
        prepareBuildGradleFile {
            """
            plugins {
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
            }
            """.trimIndent()
        }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        // first run saves configuration cache
        val result = runGradleWithArguments("generateJooqClasses", "--configuration-cache")

        // second run uses configuration cache
        val resultFromCache = runGradleWithArguments("generateJooqClasses", "--configuration-cache")

        // then
        expect {
            that(result).apply {
                generateJooqClassesTask.outcome isEqualTo SUCCESS
                get { output }.contains("Configuration cache entry stored")
            }
            that(resultFromCache).apply {
                generateJooqClassesTask.outcome isEqualTo UP_TO_DATE
                get { output }.contains("Configuration cache entry reused") not {
                    contains("Deprecated Gradle features were used in this build")
                }
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists()
        }
    }

    @Test
    fun `should respect changes to codegen xml config with configuration cache enabled`() {
        // given
        configureLocalGradleCache()
        prepareBuildGradleFile {
            """
            plugins {
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }
            
            tasks {
                generateJooqClasses {
                    schemas.set(listOf("public", "other"))
                    usingXmlConfig()
                }
            }

            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
            }
            """.trimIndent()
        }
        copyResource(
            from = "/V01__init_multiple_schemas.sql",
            to = "src/main/resources/db/migration/V01__init_multiple_schemas.sql",
        )
        copyResource(from = "/jooq-generator.xml", to = "src/main/resources/db/jooq.xml")

        // when
        // first run saves configuration cache
        val result = runGradleWithArguments("generateJooqClasses", "--configuration-cache")

        // then
        expect {
            that(result).apply {
                generateJooqClassesTask.outcome isEqualTo SUCCESS
                get { output }.contains("Configuration cache entry stored")
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/public_/tables/Foo.java"),
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/other/tables/Bar.java"),
            ).notExists()
        }

        // when second run uses configuration cache
        copyResource(from = "/jooq-generator-without-excludes.xml", to = "src/main/resources/db/jooq.xml")
        val resultFromCache = runGradleWithArguments("generateJooqClasses", "--configuration-cache")

        // then
        expect {
            that(resultFromCache).apply {
                generateJooqClassesTask.outcome isEqualTo SUCCESS
                get { output }.contains("Configuration cache entry reused")
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/public_/tables/Foo.java"),
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/other/tables/Bar.java"),
            ).exists()
        }
    }

    private fun configureLocalGradleCache() {
        writeProjectFile("settings.gradle.kts") {
            """
            buildCache {
                local {
                    directory = "${localBuildCacheDirectory.path}"
                }
            }
            """.trimIndent()
        }
    }
}
