package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.java.exists

class JavaBasedMigrationsFunctionalTest : JooqDockerPluginFunctionalTestBase() {
    @Test
    fun `should be able to use a directory with compiled Java-based migrations for code gen`() {
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
                    migrationLocations.setFromClasspath(provider { "java-based-migrations/" })
                }
            }

            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
            }
            """.trimIndent()
        }
        copyResource(from = "/classes/compiled/V01__init.class", to = "java-based-migrations/db/migration/V01__init.class")
        copyResource(from = "/classes/compiled/V02__add_bar.class", to = "java-based-migrations/db/migration/V02__add_bar.class")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java"),
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
        }
    }

    @Test
    fun `should be able to use a JAR file with Java-based migrations for code gen`() {
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
                    migrationLocations.setFromClasspath(project.files("java-based-migrations/"))
                }
            }

            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
            }
            """.trimIndent()
        }
        copyResource(from = "/jars/migrations.jar", to = "java-based-migrations/migrations.jar")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java"),
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
        }
    }

    @Test
    fun `should be able to use a Gradle configuration with Java-based migrations for code gen`() {
        // given
        writeProjectFile("settings.gradle.kts") {
            """
            include("migrations")
            """.trimIndent()
        }
        writeProjectFile("migrations/build.gradle.kts") {
            """
            import dev.monosoul.jooq.RecommendedVersions.FLYWAY_VERSION
            
            plugins {
                kotlin("jvm")
                id("dev.monosoul.jooq-docker") apply false
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                compileOnly("org.flywaydb:flyway-core:${'$'}FLYWAY_VERSION")
            }
            """.trimIndent()
        }
        prepareBuildGradleFile {
            """
            plugins {
                kotlin("jvm") version "2.2.20"
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }
            
            val migrationClasspath by configurations.creating
            
            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
                migrationClasspath(project(":migrations"))
            }
            
            tasks {
                generateJooqClasses {
                    migrationLocations.setFromClasspath(migrationClasspath, "/some/pkg")
                }
            }
            """.trimIndent()
        }
        copyResource(from = "/classes/source/V01__init.txt", to = "migrations/src/main/kotlin/some/pkg/V01__init.kt")
        copyResource(from = "/classes/source/V02__add_bar.txt", to = "migrations/src/main/kotlin/some/pkg/V02__add_bar.kt")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java"),
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
        }
    }

    @Test
    fun `should be able to use a Gradle configuration with third party JAR with Java-based migrations for code gen`() {
        // given
        writeProjectFile("settings.gradle.kts") {
            """
            include("migrations")
            """.trimIndent()
        }
        writeProjectFile("migrations/build.gradle.kts") {
            """
            import dev.monosoul.jooq.RecommendedVersions.FLYWAY_VERSION
            
            plugins {
                kotlin("jvm")
                id("dev.monosoul.jooq-docker") apply false
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation("org.flywaydb:flyway-core:${'$'}FLYWAY_VERSION")
            }
            """.trimIndent()
        }
        prepareBuildGradleFile {
            """
            plugins {
                kotlin("jvm") version "2.2.20"
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }
            
            val migrationClasspath by configurations.creating
            
            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
                migrationClasspath(files("java-based-migrations/migrations.jar"))
            }
            
            tasks {
                generateJooqClasses {
                    migrationLocations.setFromClasspath(migrationClasspath)
                }
            }
            """.trimIndent()
        }
        copyResource(from = "/jars/migrations.jar", to = "java-based-migrations/migrations.jar")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java"),
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
        }
    }

    @Test
    fun `should be able to use a module source set output with Java-based migrations for code gen`() {
        // given
        writeProjectFile("settings.gradle.kts") {
            """
            include("migrations")
            """.trimIndent()
        }
        writeProjectFile("migrations/build.gradle.kts") {
            """
            import dev.monosoul.jooq.RecommendedVersions.FLYWAY_VERSION
            
            plugins {
                kotlin("jvm")
                id("dev.monosoul.jooq-docker") apply false
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation("org.flywaydb:flyway-core:${'$'}FLYWAY_VERSION")
            }
            """.trimIndent()
        }
        prepareBuildGradleFile {
            """
            plugins {
                kotlin("jvm") version "2.2.20"
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }
            
            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
            }
            
            tasks {
                generateJooqClasses {
                    migrationLocations.setFromClasspath(
                        project(":migrations").sourceSets.main.map { it.output }, 
                        "/some/pkg"
                    )
                }
            }
            """.trimIndent()
        }
        copyResource(from = "/classes/source/V01__init.txt", to = "migrations/src/main/kotlin/some/pkg/V01__init.kt")
        copyResource(from = "/classes/source/V02__add_bar.txt", to = "migrations/src/main/kotlin/some/pkg/V02__add_bar.kt")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java"),
            ).exists().and {
                get { readText() } contains "schema version:02"
            }
        }
    }
}
