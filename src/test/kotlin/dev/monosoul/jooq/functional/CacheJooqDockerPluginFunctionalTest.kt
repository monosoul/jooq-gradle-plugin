package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists
import java.io.File

class CacheJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @TempDir
    private lateinit var localBuildCacheDirectory: File

    @Test
    fun `should load generateJooqClasses task output from cache`() {
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
        // first run loads to cache
        val resultWithoutCache = runGradleWithArguments("generateJooqClasses", "--build-cache")

        // second run uses from cache
        projectFile("build").deleteRecursively()
        val resultFromCache = runGradleWithArguments("generateJooqClasses", "--build-cache")

        // third run got changes and can't use cached output
        copyResource(from = "/V02__add_bar.sql", to = "src/main/resources/db/migration/V02__add_bar.sql")
        val resultAfterInputsChange = runGradleWithArguments("generateJooqClasses", "--build-cache")

        // then
        expect {
            that(resultWithoutCache).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(resultFromCache).generateJooqClassesTask.outcome isEqualTo FROM_CACHE
            that(resultAfterInputsChange).generateJooqClassesTask.outcome isEqualTo SUCCESS

            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/FlywaySchemaHistory.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java")
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
