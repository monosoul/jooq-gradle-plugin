package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.java.exists
import strikt.java.notExists

class UpToDateChecksJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @Test
    fun `up to date check should work for output dir`() {
        // given
        prepareBuildGradleFile {
            """
                plugins {
                    id("dev.monosoul.jooq-docker")
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
            """.trimIndent()
        }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        val firstRun = runGradleWithArguments("generateJooqClasses")
        val secondRun = runGradleWithArguments("generateJooqClasses")

        projectFile("build/generated-jooq").deleteRecursively()
        val runAfterDeletion = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(firstRun).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(secondRun).generateJooqClassesTask.outcome isEqualTo UP_TO_DATE
            that(runAfterDeletion).generateJooqClassesTask.outcome isEqualTo SUCCESS
        }
    }

    @Test
    fun `up to date check should work for input dir`() {
        // given
        prepareBuildGradleFile {
            """
                plugins {
                    id("dev.monosoul.jooq-docker")
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
            """.trimIndent()
        }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        val firstRun = runGradleWithArguments("generateJooqClasses")
        val secondRun = runGradleWithArguments("generateJooqClasses")

        copyResource(from = "/V02__add_bar.sql", to = "src/main/resources/db/migration/V02__add_bar.sql")
        val runAfterNewFile = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(firstRun).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(secondRun).generateJooqClassesTask.outcome isEqualTo UP_TO_DATE
            that(runAfterNewFile).generateJooqClassesTask.outcome isEqualTo SUCCESS
        }
    }

    @Test
    fun `up to date check should work for extension changes`() {
        // given
        val initialBuildScript = """
            plugins {
                id("dev.monosoul.jooq-docker")
            }

            jooq {
                image {
                    tag = "11.2-alpine"
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                jdbc("org.postgresql:postgresql:42.3.6")
            }
        """.trimIndent()

        val extensionValueChangedBuildScript = """
            plugins {
                id("dev.monosoul.jooq-docker")
            }

            jooq {
                image {
                    tag = "11.3-alpine"
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                jdbc("org.postgresql:postgresql:42.3.6")
            }
        """.trimIndent()

        prepareBuildGradleFile { initialBuildScript }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        val initialResult = runGradleWithArguments("generateJooqClasses")

        prepareBuildGradleFile { extensionValueChangedBuildScript }
        val resultAfterChangeToExtension = runGradleWithArguments("generateJooqClasses")

        val finalResultNoChanges = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(initialResult).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(resultAfterChangeToExtension).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(finalResultNoChanges).generateJooqClassesTask.outcome isEqualTo UP_TO_DATE
        }
    }

    @Test
    fun `up to date check should work for generator customizations`() {
        // given
        val initialBuildScript = """
            plugins {
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }

            tasks {
                generateJooqClasses {
                    schemas = arrayOf("public", "other")
                    usingJavaConfig {
                        database.withExcludes("BAR")
                    }
                }
            }

            dependencies {
                jdbc("org.postgresql:postgresql:42.3.6")
            }
        """.trimIndent()

        val updatedBuildScript = """
            plugins {
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }

            tasks {
                generateJooqClasses {
                    schemas = arrayOf("public", "other")
                    usingJavaConfig {
                        database.withExcludes(".*")
                    }
                }
            }

            dependencies {
                jdbc("org.postgresql:postgresql:42.3.6")
            }
        """.trimIndent()

        prepareBuildGradleFile { initialBuildScript }
        copyResource(
            from = "/V01__init_multiple_schemas.sql",
            to = "src/main/resources/db/migration/V01__init_multiple_schemas.sql"
        )

        // when
        val initialResult = runGradleWithArguments("generateJooqClasses")

        prepareBuildGradleFile { updatedBuildScript }
        val resultAfterUpdate = runGradleWithArguments("generateJooqClasses")

        val finalResultNoChanges = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(initialResult).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(resultAfterUpdate).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(finalResultNoChanges).generateJooqClassesTask.outcome isEqualTo UP_TO_DATE

            that(
                projectFile("build/generated-jooq/org/jooq/generated/public_/tables/Foo.java")
            ).notExists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
            ).notExists()
        }
    }

    @Test
    fun `should regenerate jooq classes when out of date even if output directory already has classes generated`() {
        // given
        val initialBuildScript = """
            import org.jooq.meta.jaxb.ForcedType

            plugins {
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }

            tasks {
                generateJooqClasses {
                    usingJavaConfig {
                        database.withForcedTypes(ForcedType()
                            .withUserType("com.example.UniqueClassForFirstGeneration")
                            .withBinding("com.example.PostgresJSONGsonBinding")
                            .withTypes("JSONB"))
                    }
                }
            }

            dependencies {
                jdbc("org.postgresql:postgresql:42.3.6")
            }
        """.trimIndent()

        val updatedBuildScript = """
            import org.jooq.meta.jaxb.ForcedType

            plugins {
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }

            tasks {
                generateJooqClasses {
                    usingJavaConfig {
                        database.withForcedTypes(ForcedType()
                            .withUserType("com.example.UniqueClassForSecondGeneration")
                            .withBinding("com.example.PostgresJSONGsonBinding")
                            .withTypes("JSONB"))
                    }
                }
            }

            dependencies {
                jdbc("org.postgresql:postgresql:42.3.6")
            }
        """.trimIndent()

        prepareBuildGradleFile { initialBuildScript }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        val initialResult = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(initialResult).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists().get { readText() }.contains("com.example.UniqueClassForFirstGeneration")
        }

        // when
        prepareBuildGradleFile { updatedBuildScript }
        val resultAfterUpdate = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(resultAfterUpdate).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists().get { readText() }.contains("com.example.UniqueClassForSecondGeneration")
        }
    }
}
