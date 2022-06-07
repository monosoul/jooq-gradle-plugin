package dev.monosoul.jooq.functional

import dev.monosoul.jooq.container.PostgresContainer
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists

class RunWithExternalDatabaseJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    private val postgresContainer = PostgresContainer()

    @BeforeEach
    fun startPostgres() {
        postgresContainer.start()
    }

    @AfterEach
    fun stopPostgres() {
        postgresContainer.stop()
    }

    @Test
    fun `should support using external DB for classes generation`() {
        // given
        prepareBuildGradleFile {
            """
                plugins {
                    id("dev.monosoul.jooq-docker")
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
                        }
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
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/FlywaySchemaHistory.java")
            ).exists()
        }
    }
}
