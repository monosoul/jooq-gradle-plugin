package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists

class FlywayConfigurationJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @Test
    fun `should override flyway configuration with flywayProperties task input`() {
        // given
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
                        flywayProperties = mapOf("flyway.placeholderReplacement" to "false")
                    }
                }

                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
            """.trimIndent()
        }
        copyResource(
            from = "/V01__init_with_placeholders.sql",
            to = "src/main/resources/db/migration/V01__init_with_placeholders.sql"
        )

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
        }
    }

    @Test
    fun `schema version provider should be aware of flyway table name override`() {
        // given
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
                        flywayProperties = mapOf("flyway.table" to "some_schema_table")
                    }
                }

                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
            """.trimIndent()
        }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/SomeSchemaTable.java")
            ).exists()
        }
    }

    @Test
    fun `should generate flyway table in first schema by default`() {
        // given
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
                        schemas = arrayOf("other", "public")
                    }
                }

                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
            """.trimIndent()
        }
        copyResource(
            from = "/V01__init_multiple_schemas.sql",
            to = "src/main/resources/db/migration/V01__init_multiple_schemas.sql"
        )

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/public_/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/other/tables/FlywaySchemaHistory.java")
            ).exists()
        }
    }
}
