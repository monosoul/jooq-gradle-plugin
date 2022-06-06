package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists

class ConfigurabilityJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @Test
    fun `should generate jooq classes for PostgreSQL db with default config for multiple schemas`() {
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
                projectFile("build/generated-jooq/org/jooq/generated/public_/tables/FlywaySchemaHistory.java")
            ).exists()
        }
    }

    @Test
    fun `should generate jooq classes for PostgreSQL db with default config for multiple schemas and renames package`() {
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
                        schemaToPackageMapping = mapOf("public" to "fancy_name")
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
                projectFile("build/generated-jooq/org/jooq/generated/fancy_name/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
            ).exists()
        }
    }

    @Test
    fun `should generate jooq classes in a given package`() {
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
                        basePackageName = "com.example"
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
                projectFile("build/generated-jooq/com/example/tables/Foo.java")
            ).exists()
        }
    }

    @Test
    fun `should pass output schema to default setting to jOOQ generator`() {
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
                        outputSchemaToDefault = setOf("public")
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
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/DefaultSchema.java")
            ).exists()
        }
    }

    @Test
    fun `should respect outputDirectory task property`() {
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
                        outputDirectory.set(project.layout.buildDirectory.dir("gen"))
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
                projectFile("build/gen/org/jooq/generated/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/gen/org/jooq/generated/tables/FlywaySchemaHistory.java")
            ).exists()
        }
    }
}
