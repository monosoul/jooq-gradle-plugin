package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.java.exists
import strikt.java.notExists

class ConfigurabilityJooqDockerPluginFunctionalTest : dev.monosoul.jooq.functional.JooqDockerPluginFunctionalTestBase() {
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
                jooqCodegen("org.postgresql:postgresql:42.3.6")
            }
            """.trimIndent()
        }
        copyResource(
            from = "/V01__init_multiple_schemas.sql",
            to = "src/main/resources/db/migration/V01__init_multiple_schemas.sql",
        )

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/public_/tables/Foo.java"),
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/other/tables/Bar.java"),
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
                    schemaToPackageMapping.put("public", "fancy_name")
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

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/fancy_name/tables/Foo.java"),
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/other/tables/Bar.java"),
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
                    basePackageName.set("com.example")
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
                projectFile("build/generated-jooq/com/example/tables/Foo.java"),
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
                    outputSchemaToDefault.add("public")
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

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/DefaultSchema.java"),
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
                jooqCodegen("org.postgresql:postgresql:42.3.6")
            }
            """.trimIndent()
        }
        copyResource(
            from = "/V01__init_multiple_schemas.sql",
            to = "src/main/resources/db/migration/V01__init_multiple_schemas.sql",
        )

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/gen/org/jooq/generated/tables/Foo.java"),
            ).exists()
        }
    }

    @Test
    fun `should respect targetSourceSet task property`() {
        // given
        prepareBuildGradleFile {
            """
            import dev.monosoul.jooq.RecommendedVersions
                
            plugins {
                kotlin("jvm") version "2.2.20"
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }
            
            sourceSets {
                create("custom") {
                    compileClasspath = files(main.map { it.compileClasspath })
                    runtimeClasspath = files(main.map { it.runtimeClasspath })
                }
            }

            tasks.generateJooqClasses {
                targetSourceSet.set("custom")
            }

            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
                
                implementation("org.postgresql:postgresql:42.5.4")
                implementation("org.jooq:jooq:${'$'}{RecommendedVersions.JOOQ_VERSION}")
                implementation("org.flywaydb:flyway-core:${'$'}{RecommendedVersions.FLYWAY_VERSION}")
            }
            """.trimIndent()
        }
        copyResource(
            from = "/V01__init_multiple_schemas.sql",
            to = "src/main/resources/db/migration/V01__init_multiple_schemas.sql",
        )

        // when
        val mainSourceSetCompilationResult = runGradleWithArguments("classes")

        // then
        expect {
            that(mainSourceSetCompilationResult).getTask("generateJooqClasses").isNull()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).notExists()
        }

        // and when
        val testSourceSetCompilationResult = runGradleWithArguments("customClasses")

        // then
        expect {
            that(testSourceSetCompilationResult).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists()
        }
    }
}
