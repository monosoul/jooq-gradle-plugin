package dev.monosoul.jooq.functional

import dev.monosoul.jooq.container.PostgresContainer
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.java.exists
import strikt.java.notExists

class GroovyBuildscriptJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @Test
    fun `should respect global configuration regardless of declaration order`() {
        // given
        prepareBuildGradleFile("build.gradle") {
            // language=gradle
            """
                plugins {
                    id "dev.monosoul.jooq-docker"
                }

                repositories {
                    mavenCentral()
                }
                
                tasks {
                    generateJooqClasses {
                        flywayProperties.put("flyway.placeholderReplacement", "false")
                    }
                }
                
                jooq {
                    withContainer {
                        image {
                            name = "postgres:13.4-alpine"
                        }
                    }
                }

                dependencies {
                    jooqCodegen "org.postgresql:postgresql:42.3.6"
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
            that(result).apply {
                generateJooqClassesTask.outcome isEqualTo SUCCESS
                get { output } contains "postgres:13.4-alpine"
            }
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
        }
    }

    @Test
    fun `should work with Groovy buildscript when running with a container`() {
        // given
        prepareBuildGradleFile("build.gradle") {
            // language=gradle
            """
                plugins {
                    id "dev.monosoul.jooq-docker"
                }

                repositories {
                    mavenCentral()
                }
                
                jooq {
                    withContainer {
                        db {
                            username = "customusername"
                            password = "custompassword"
                            
                            jdbc {
                                schema = "jdbc:postgresql"
                            }
                        }
                        image {
                            envVars = [
                                "POSTGRES_USER": "customusername",
                                "POSTGRES_PASSWORD": "custompassword",
                                "POSTGRES_DB": "postgres"
                            ]
                        }
                    }
                }

                tasks {
                    generateJooqClasses {
                        flywayProperties.put("flyway.placeholderReplacement", "false")
                        usingJavaConfig {
                            database.withExcludes("BAR")
                        }
                    }
                }

                dependencies {
                    jooqCodegen "org.postgresql:postgresql:42.3.6"
                }
            """.trimIndent()
        }
        copyResource(
            from = "/V01__init_with_placeholders.sql",
            to = "src/main/resources/db/migration/V01__init_with_placeholders.sql"
        )
        copyResource(from = "/V02__add_bar.sql", to = "src/main/resources/db/migration/V02__add_bar.sql")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java")
            ).notExists()
        }
    }

    @Test
    fun `should work with Groovy buildscript when running with an external DB`() {
        // given
        val postgresContainer = PostgresContainer().also { it.start() }
        prepareBuildGradleFile("build.gradle") {
            // language=gradle
            """
                plugins {
                    id "dev.monosoul.jooq-docker"
                }

                repositories {
                    mavenCentral()
                }
                
                jooq {
                    withoutContainer {
                        db {
                            username = "${postgresContainer.username}"
                            password = "${postgresContainer.password}"
                            name = "${postgresContainer.databaseName}"
                            host = "${postgresContainer.host}"
                            port = ${postgresContainer.firstMappedPort}
                            
                            jdbc {
                                schema = "jdbc:postgresql"
                            }
                        }
                    }
                }

                tasks {
                    generateJooqClasses {
                        flywayProperties.put("flyway.placeholderReplacement", "false")
                        usingJavaConfig {
                            database.withExcludes("BAR")
                        }
                    }
                }

                dependencies {
                    jooqCodegen "org.postgresql:postgresql:42.3.6"
                }
            """.trimIndent()
        }
        copyResource(
            from = "/V01__init_with_placeholders.sql",
            to = "src/main/resources/db/migration/V01__init_with_placeholders.sql"
        )
        copyResource(from = "/V02__add_bar.sql", to = "src/main/resources/db/migration/V02__add_bar.sql")

        // when
        val result = try {
            runGradleWithArguments("generateJooqClasses")
        } finally {
            postgresContainer.stop()
        }

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java")
            ).notExists()
        }
    }

    @Test
    fun `should work with Groovy buildscript when using XML generator definition`() {
        // given
        prepareBuildGradleFile("build.gradle") {
            // language=gradle
            """
                plugins {
                    id "dev.monosoul.jooq-docker"
                }

                repositories {
                    mavenCentral()
                }

                tasks {
                    generateJooqClasses {
                        flywayProperties.put("flyway.placeholderReplacement", "false")
                        usingXmlConfig(project.file("src/main/resources/db/jooq.xml")) {
                            database.withExcludes("BAR")
                        }
                    }
                }

                dependencies {
                    jooqCodegen "org.postgresql:postgresql:42.3.6"
                }
            """.trimIndent()
        }
        copyResource(
            from = "/V01__init_with_placeholders.sql",
            to = "src/main/resources/db/migration/V01__init_with_placeholders.sql"
        )
        copyResource(from = "/V02__add_bar.sql", to = "src/main/resources/db/migration/V02__add_bar.sql")
        copyResource(from = "/jooq-generator.xml", to = "src/main/resources/db/jooq.xml")

        // when
        val result = runGradleWithArguments("generateJooqClasses")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Bar.java")
            ).notExists()
        }
    }

    @Test
    fun `should be able generate jooq classes for internal and external databases with Groovy buildscript`() {
        // given
        val postgresContainer = PostgresContainer().also { it.start() }
        prepareBuildGradleFile("build.gradle") {
            // language=gradle
            """
                import dev.monosoul.jooq.GenerateJooqClassesTask
                import dev.monosoul.jooq.RecommendedVersions
                
                plugins {
                    id "org.jetbrains.kotlin.jvm" version "1.6.21"
                    id "dev.monosoul.jooq-docker"
                }

                repositories {
                    mavenCentral()
                }
                
                tasks {
                    generateJooqClasses {
                        basePackageName.set("org.jooq.generated.local")
                        outputDirectory.set(project.layout.buildDirectory.dir("local"))
                    }
                }
                
                tasks.register('generateJooqClassesForExternal', GenerateJooqClassesTask) {
                    basePackageName.set("org.jooq.generated.remote")
                    outputDirectory.set(project.layout.buildDirectory.dir("remote"))
                    includeFlywayTable.set(true)
                    
                    withoutContainer {
                        db {
                            username = "${postgresContainer.username}"
                            password = "${postgresContainer.password}"
                            name = "${postgresContainer.databaseName}"
                            host = "${postgresContainer.host}"
                            port = ${postgresContainer.firstMappedPort}
                        }
                    }
                }

                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib")
                    jooqCodegen("org.postgresql:postgresql:42.3.6")
                    implementation("org.jooq:jooq:" + RecommendedVersions.JOOQ_VERSION)
                }
            """.trimIndent()
        }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")

        // when
        val result = runGradleWithArguments("classes")
        postgresContainer.stop()

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/local/org/jooq/generated/local/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/remote/org/jooq/generated/remote/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/remote/org/jooq/generated/remote/tables/FlywaySchemaHistory.java")
            ).exists()
        }
    }
}
