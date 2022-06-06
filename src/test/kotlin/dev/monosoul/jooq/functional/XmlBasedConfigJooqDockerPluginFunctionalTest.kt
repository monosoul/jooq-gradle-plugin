package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists
import strikt.java.notExists

class XmlBasedConfigJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @Test
    fun `should generate jOOQ classes using XML generator definition`() {
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
                        schemas.set(listOf("public", "other"))
                        usingXmlConfig()
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
        copyResource(from = "/jooq-generator.xml", to = "src/main/resources/db/jooq.xml")

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
            ).notExists()
        }
    }

    @Test
    fun `should apply customizations to XML-based code generation configuration`() {
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
                        schemas.set(listOf("public", "other"))
                        usingXmlConfig {
                            database.withExcludes("BAR")
                        }
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
        copyResource(from = "/jooq-generator-without-excludes.xml", to = "src/main/resources/db/jooq.xml")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).getTaskOutcome("generateJooqClasses") isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/public_/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
            ).notExists()
        }
    }
}
