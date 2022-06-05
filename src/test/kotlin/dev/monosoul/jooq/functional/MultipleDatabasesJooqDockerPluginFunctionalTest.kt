package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists

class MultipleDatabasesJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @Test
    fun `should generate jooq classes for PostgreSQL and MySQL`() {
        // given
        prepareBuildGradleFile {
            """
                import dev.monosoul.jooq.GenerateJooqClassesTask
                
                plugins {
                    kotlin("jvm") version "1.6.21"
                    id("dev.monosoul.jooq-docker")
                }

                repositories {
                    mavenCentral()
                }
                
                tasks {
                    generateJooqClasses {
                        basePackageName = "org.jooq.generated.postgres"
                        inputDirectory.setFrom("src/main/resources/postgres/migration")
                        outputDirectory.set(project.layout.buildDirectory.dir("postgres"))
                    }
                    
                    register<GenerateJooqClassesTask>("generateJooqClassesForMySql") {
                        basePackageName = "org.jooq.generated.mysql"
                        inputDirectory.setFrom("src/main/resources/mysql/migration")
                        outputDirectory.set(project.layout.buildDirectory.dir("mysql"))
                    
                        withContainer {
                            image {
                                name = "mysql:8.0.29"
                                envVars = mapOf(
                                    "MYSQL_ROOT_PASSWORD" to "mysql",
                                    "MYSQL_DATABASE" to "mysql"
                                )
                            }
                            db {
                                username = "root"
                                password = "mysql"
                                name = "mysql"
                                port = 3306
                            
                                jdbc {
                                    schema = "jdbc:mysql"
                                    driverClassName = "com.mysql.cj.jdbc.Driver"
                                }
                            }
                        }
                    }
                }

                dependencies {
                    implementation(kotlin("stdlib"))
                    jdbc("org.postgresql:postgresql:42.3.6")
                    jdbc("mysql:mysql-connector-java:8.0.29")
                    implementation("org.jooq:jooq:3.16.6")
                }
            """.trimIndent()
        }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/postgres/migration/V01__init.sql")
        copyResource(from = "/V01__init_mysql.sql", to = "src/main/resources/mysql/migration/V01__init.sql")

        // when
        val result = runGradleWithArguments("tasks", "classes")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/postgres/org/jooq/generated/postgres/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/postgres/org/jooq/generated/postgres/tables/FlywaySchemaHistory.java")
            ).exists()
            that(
                projectFile("build/mysql/org/jooq/generated/mysql/tables/Foo.java")
            ).exists()
            that(
                projectFile("build/mysql/org/jooq/generated/mysql/tables/FlywaySchemaHistory.java")
            ).exists()
        }
    }

    @Test
    fun `local task configuration should inherit global configuration`() {
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
                    withContainer {
                        image {
                            name = "mysql:8.0.29"
                            envVars = mapOf(
                                "MYSQL_ROOT_PASSWORD" to "mysql",
                                "MYSQL_DATABASE" to "mysql",
                            )
                        }
                        db {
                            username = "root"
                            password = "mysql"
                            name = "mysql"
                            port = 6666
                            
                            jdbc {
                                schema = "jdbc:mysql"
                                driverClassName = "com.mysql.cj.jdbc.Driver"
                            }
                        }
                    }
                }
                
                tasks {
                    generateJooqClasses {
                        withContainer {
                            image {
                                envVars = envVars + mapOf(
                                    "MYSQL_TCP_PORT" to "6666"
                                )
                            }
                        }
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
