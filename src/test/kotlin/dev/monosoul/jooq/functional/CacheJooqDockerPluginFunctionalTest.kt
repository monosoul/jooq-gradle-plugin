package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists
import java.io.File

class CacheJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {
    @TempDir
    private lateinit var localBuildCacheDirectory: File

    @TempDir
    private lateinit var siblingProjectDir: File

    @BeforeEach
    fun siblingSetUp() {
        siblingProjectDir.copy(from = "/testkit-gradle.properties", to = "gradle.properties")
    }

    @Test
    fun `should load generateJooqClasses task output from cache`() {
        // given
        writeProjectFile("settings.gradle.kts") {
            """
            buildCache {
                local {
                    directory = "${localBuildCacheDirectory.path}"
                }
            }
            """.trimIndent()
        }

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
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java"),
            ).exists()
        }
    }

    @Test
    fun `sibling project in a different directory should have the same cache hash`() {
        // given
        """
        buildCache {
            local {
                directory = "${localBuildCacheDirectory.path}"
            }
        }
        """.trimIndent().also {
            writeProjectFile("settings.gradle.kts") { it }
            siblingProjectDir.writeBuildGradleFile("settings.gradle.kts") { it }
        }

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
        """.trimIndent().also {
            prepareBuildGradleFile { it }
            siblingProjectDir.writeBuildGradleFile { it }
        }

        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")
        siblingProjectDir.copy(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        // run from the first project loads to cache
        val resultWithoutCache = runGradleWithArguments("generateJooqClasses", "--build-cache")

        // run from the sibling project uses the cache
        val resultFromCache =
            runGradleWithArguments(
                "generateJooqClasses",
                "--build-cache",
                projectDirectory = siblingProjectDir,
            )

        // then
        expect {
            that(resultWithoutCache).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(resultFromCache).generateJooqClassesTask.outcome isEqualTo FROM_CACHE

            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists()
            that(
                siblingProjectDir.getChild("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists()
        }
    }
}
