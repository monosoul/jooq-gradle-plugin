package dev.monosoul.jooq.functional

import dev.monosoul.jooq.container.PostgresContainer
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists

class PropertiesConfigurationJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @Test
    fun `should support with container override to without container`() {
        // given
        val postgresContainer = PostgresContainer().also { it.start() }
        prepareBuildGradleFile {
            """
                plugins {
                    id("dev.monosoul.jooq-docker")
                }
                
                jooq {
                    withContainer {
                        image {
                            command = "postgres -p 6666"
                        }
                    }
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
        val result = runGradleWithArguments(
            "generateJooqClasses",
            "-Pdev.monosoul.jooq.withoutContainer.db.username=${postgresContainer.username}",
            "-Pdev.monosoul.jooq.withoutContainer.db.password=${postgresContainer.password}",
            "-Pdev.monosoul.jooq.withoutContainer.db.name=${postgresContainer.databaseName}",
            "-Pdev.monosoul.jooq.withoutContainer.db.port=${postgresContainer.firstMappedPort}"
        )
        postgresContainer.stop()

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

    @Test
    fun `should support partial default configuration override via properties`() {
        // given
        prepareBuildGradleFile {
            """
                plugins {
                    id("dev.monosoul.jooq-docker")
                }
                
                jooq {
                    withContainer {
                        image {
                            command = "postgres -p 6666"
                        }
                    }
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
        val result = runGradleWithArguments(
            "generateJooqClasses",
            "-Pdev.monosoul.jooq.withContainer.db.port=6666",
        )

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

    @Test
    fun `should support customized configuration override via properties`() {
        // given
        prepareBuildGradleFile {
            """
                plugins {
                    id("dev.monosoul.jooq-docker")
                }
                
                jooq {
                    withContainer {
                        image {
                            command = "postgres -p 6666"
                        }
                    }
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
        val result = runGradleWithArguments(
            "generateJooqClasses",
            "-Pdev.monosoul.jooq.withContainer.image.command=",
        )

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

    @Test
    fun `should be possible to configure the plugin with properties to run with container`() {
        // given
        writeProjectFile("gradle.properties") {
            """
                dev.monosoul.jooq.withContainer.db.username=root
                dev.monosoul.jooq.withContainer.db.password=mysql
                dev.monosoul.jooq.withContainer.db.name=mysql
                dev.monosoul.jooq.withContainer.db.port=3306
                dev.monosoul.jooq.withContainer.db.jdbc.schema=jdbc:mysql
                dev.monosoul.jooq.withContainer.db.jdbc.driverClassName=com.mysql.cj.jdbc.Driver
                dev.monosoul.jooq.withContainer.db.jdbc.urlQueryParams=?useSSL=false
                dev.monosoul.jooq.withContainer.image.name=mysql:8.0.29
                dev.monosoul.jooq.withContainer.image.testQuery=SELECT 2
                dev.monosoul.jooq.withContainer.image.command=--default-authentication-plugin=mysql_native_password
                dev.monosoul.jooq.withContainer.image.envVars.MYSQL_ROOT_PASSWORD=mysql
                dev.monosoul.jooq.withContainer.image.envVars.MYSQL_DATABASE=mysql
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
                    jdbc("mysql:mysql-connector-java:8.0.29")
                }
            """.trimIndent()
        }
        copyResource(from = "/V01__init_mysql.sql", to = "src/main/resources/db/migration/V01__init_mysql.sql")

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
    fun `should be possible to configure the plugin with properties to run without container`() {
        // given
        val postgresContainer = PostgresContainer().also { it.start() }
        writeProjectFile("gradle.properties") {
            """
                dev.monosoul.jooq.withoutContainer.db.username=${postgresContainer.username}
                dev.monosoul.jooq.withoutContainer.db.password=${postgresContainer.password}
                dev.monosoul.jooq.withoutContainer.db.name=${postgresContainer.databaseName}
                dev.monosoul.jooq.withoutContainer.db.port=${postgresContainer.firstMappedPort}
                dev.monosoul.jooq.withoutContainer.db.jdbc.schema=jdbc:postgresql
                dev.monosoul.jooq.withoutContainer.db.jdbc.driverClassName=org.postgresql.Driver
                dev.monosoul.jooq.withoutContainer.db.jdbc.urlQueryParams=?loggerLevel=OFF
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
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
            """.trimIndent()
        }
        copyResource(from = "/V01__init_mysql.sql", to = "src/main/resources/db/migration/V01__init_mysql.sql")

        // when
        val result = runGradleWithArguments("generateJooqClasses")
        postgresContainer.stop()

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
            ).exists()
        }
    }
}
