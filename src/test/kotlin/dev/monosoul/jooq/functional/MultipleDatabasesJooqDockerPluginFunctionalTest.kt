package dev.monosoul.jooq.functional

import dev.monosoul.jooq.container.PostgresContainer
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
            import dev.monosoul.jooq.RecommendedVersions
            import dev.monosoul.jooq.migration.MigrationLocation
            
            plugins {
                kotlin("jvm") version "1.6.21"
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }
            
            tasks {
                generateJooqClasses {
                    basePackageName.set("org.jooq.generated.postgres")
                    migrationLocations.setFromFilesystem("src/main/resources/postgres/migration")
                    outputDirectory.set(project.layout.buildDirectory.dir("postgres"))
                }
                
                register<GenerateJooqClassesTask>("generateJooqClassesForMySql") {
                    basePackageName.set("org.jooq.generated.mysql")
                    migrationLocations.setFromFilesystem(project.files("src/main/resources/mysql/migration"))
                    outputDirectory.set(project.layout.buildDirectory.dir("mysql"))
                    includeFlywayTable.set(true)
                
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
                jooqCodegen("org.postgresql:postgresql:42.3.6")
                jooqCodegen("mysql:mysql-connector-java:8.0.29")
                jooqCodegen("org.flywaydb:flyway-mysql:${'$'}{RecommendedVersions.FLYWAY_VERSION}")
                jooqCodegen("org.flywaydb:flyway-database-postgresql:${'$'}{RecommendedVersions.FLYWAY_VERSION}")
                implementation("org.jooq:jooq:${'$'}{RecommendedVersions.JOOQ_VERSION}")
            }
            """.trimIndent()
        }
        copyResource(from = "/V01__init.sql", to = "src/main/resources/postgres/migration/V01__init.sql")
        copyResource(from = "/V01__init_mysql.sql", to = "src/main/resources/mysql/migration/V01__init.sql")

        // when
        val result = runGradleWithArguments("classes")

        // then
        expect {
            that(result).generateJooqClassesTask.outcome isEqualTo SUCCESS
            that(
                projectFile("build/classes/java/main/org/jooq/generated/postgres/tables/Foo.class"),
            ).exists()
            that(
                projectFile("build/classes/java/main/org/jooq/generated/mysql/tables/Foo.class"),
            ).exists()
            that(
                projectFile("build/classes/java/main/org/jooq/generated/mysql/tables/FlywaySchemaHistory.class"),
            ).exists()
        }
    }

    @Test
    fun `local task configuration should inherit global configuration when running with container`() {
        // given
        prepareBuildGradleFile {
            """
            import dev.monosoul.jooq.RecommendedVersions
            
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
                jooqCodegen("mysql:mysql-connector-java:8.0.29")
                jooqCodegen("org.flywaydb:flyway-mysql:${'$'}{RecommendedVersions.FLYWAY_VERSION}")
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
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists()
        }
    }

    @Test
    fun `local task configuration should inherit global configuration when running without container`() {
        // given
        val postgresContainer = PostgresContainer().also { it.start() }
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
                        port = 6666
                    }
                }
            }
            
            tasks {
                generateJooqClasses {
                    withoutContainer {
                        db {
                            port = ${postgresContainer.firstMappedPort}
                        }
                    }
                }
            }

            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
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
                projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java"),
            ).exists()
        }
    }

    @Test
    fun `should be able generate jooq classes for internal and external databases`() {
        // given
        val postgresContainer = PostgresContainer().also { it.start() }
        prepareBuildGradleFile {
            """
            import dev.monosoul.jooq.GenerateJooqClassesTask
            import dev.monosoul.jooq.RecommendedVersions
            
            plugins {
                kotlin("jvm") version "1.6.21"
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }
            
            tasks {
                generateJooqClasses {
                    basePackageName.set("org.jooq.generated.local")
                    outputDirectory.set(project.layout.buildDirectory.dir("local"))
                }
                
                register<GenerateJooqClassesTask>("generateJooqClassesForExternal") {
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
            }

            dependencies {
                implementation(kotlin("stdlib"))
                jooqCodegen("org.postgresql:postgresql:42.3.6")
                implementation("org.jooq:jooq:${'$'}{RecommendedVersions.JOOQ_VERSION}")
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
                projectFile("build/classes/java/main/org/jooq/generated/local/tables/Foo.class"),
            ).exists()
            that(
                projectFile("build/classes/java/main/org/jooq/generated/remote/tables/Foo.class"),
            ).exists()
            that(
                projectFile("build/classes/java/main/org/jooq/generated/remote/tables/FlywaySchemaHistory.class"),
            ).exists()
        }
    }
}
