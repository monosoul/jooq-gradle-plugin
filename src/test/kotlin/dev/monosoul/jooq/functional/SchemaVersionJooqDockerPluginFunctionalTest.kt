package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.java.exists

class SchemaVersionJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

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
                        flywayProperties.put("flyway.table", "some_schema_table")
                    }
                }

                dependencies {
                    jooqCodegen("org.postgresql:postgresql:42.3.6")
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
            ).exists().and {
                get { readText() } contains "schema version:01"
            }
        }
    }

    @Test
    fun `generated jOOQ classes should have schema version in the Generated annotation`() {
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
                    jooqCodegen("org.postgresql:postgresql:42.3.6")
                }
            """.trimIndent()
        }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")
        copyResource(from = "/V02__add_bar.sql", to = "src/main/resources/db/migration/V02__add_bar.sql")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java").also {
                    println(it.readText())
                }
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java")
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/FlywaySchemaHistory.java")
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
        }
    }
}
