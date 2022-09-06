package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.java.exists
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
                generateJooqClassesTask.outcome isEqualTo SUCCESS
                get { output }.contains("Configuration cache entry reused")
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
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
