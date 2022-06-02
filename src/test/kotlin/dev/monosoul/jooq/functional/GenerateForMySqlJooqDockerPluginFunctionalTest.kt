package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists

class GenerateForMySqlJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @Test
    fun `should be able to generate jOOQ classes for MySQL`() {
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
                    image {
                        repository = "mysql"
                        tag = "8.0.29"
                        envVars = mapOf(
                            "MYSQL_ROOT_PASSWORD" to "mysql",
                            "MYSQL_DATABASE" to "mysql"
                        )
                        command = "--default-authentication-plugin=mysql_native_password"
                    }

                    db {
                        username = "root"
                        password = "mysql"
                        name = "mysql"
                        port = 3306
                    }

                    jdbc {
                        schema = "jdbc:mysql"
                        driverClassName = "com.mysql.cj.jdbc.Driver"
                        jooqMetaName = "org.jooq.meta.mysql.MySQLDatabase"
                        urlQueryParams = "?useSSL=false"
                    }
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
}
