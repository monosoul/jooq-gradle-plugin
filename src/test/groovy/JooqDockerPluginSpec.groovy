import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Paths

import static org.gradle.testkit.runner.TaskOutcome.*

class JooqDockerPluginSpec extends Specification {

    @TempDir
    File projectDir

    @TempDir
    File localBuildCacheDirectory

    def setup() {
        copyResource("testkit-gradle.properties", "gradle.properties")
    }

    def "plugin is applicable"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      """)

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .build()

        then:
        result != null
    }

    def "generates jooq classes for PostgreSQL db with default config"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def generatedFlywayClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/FlywaySchemaHistory.java")
        Files.exists(generatedFooClass)
        Files.exists(generatedFlywayClass)
    }

    def "generates jooq classes for PostgreSQL db with default config for multiple schemas"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              schemas = arrayOf("public", "other")
                          }
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init_multiple_schemas.sql", "src/main/resources/db/migration/V01__init_multiple_schemas.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedPublic = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/public_/tables/Foo.java")
        def generatedOther = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
        def generatedFlywaySchemaClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/public_/tables/FlywaySchemaHistory.java")
        Files.exists(generatedPublic)
        Files.exists(generatedOther)
        Files.exists(generatedFlywaySchemaClass)
    }

    def "generates jooq classes for PostgreSQL db with default config for multiple schemas and renames package"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              schemas = arrayOf("public", "other")
                              schemaToPackageMapping = mapOf("public" to "fancy_name")
                          }
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init_multiple_schemas.sql", "src/main/resources/db/migration/V01__init_multiple_schemas.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedPublic = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/fancy_name/tables/Foo.java")
        def generatedOther = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
        Files.exists(generatedPublic)
        Files.exists(generatedOther)
    }

    def "respects the generator customizations"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              schemas = arrayOf("public", "other")
                              customizeGenerator {
                                  database.withExcludes("BAR")
                              }
                          }
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init_multiple_schemas.sql", "src/main/resources/db/migration/V01__init_multiple_schemas.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedPublic = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/public_/tables/Foo.java")
        def generatedOther = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
        Files.exists(generatedPublic)
        !Files.exists(generatedOther)
    }

    def "up to date check works for output dir"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def firstRun = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()
        def secondRun = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()
        Paths.get(projectDir.path, "build/generated-jooq").toFile().deleteDir()
        def runAfterDeletion = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        firstRun.task(":generateJooqClasses").outcome == SUCCESS
        secondRun.task(":generateJooqClasses").outcome == UP_TO_DATE
        runAfterDeletion.task(":generateJooqClasses").outcome == SUCCESS
    }

    def "up to date check works for input dir"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def firstRun = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()
        def secondRun = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()
        copyResource("/V02__add_bar.sql", "src/main/resources/db/migration/V02__add_bar.sql")
        def runAfterDeletion = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        firstRun.task(":generateJooqClasses").outcome == SUCCESS
        secondRun.task(":generateJooqClasses").outcome == UP_TO_DATE
        runAfterDeletion.task(":generateJooqClasses").outcome == SUCCESS
    }

    def "up to date check works for extension changes"() {
        given:
        def initialBuildGradle =
                """
                plugins {
                    id("dev.monosoul.jooq-docker")
                }
                
                jooq {
                    image {
                        tag = "11.2-alpine"
                    }
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
                """
        def extensionUpdatedBuildGradle =
                """
                plugins {
                    id("dev.monosoul.jooq-docker")
                }
                
                jooq {
                    image {
                        tag = "11.3-alpine"
                    }
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
                """
        prepareBuildGradleFile(initialBuildGradle)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def initialResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()
        prepareBuildGradleFile(extensionUpdatedBuildGradle)
        def resultAfterChangeToExtension = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()
        def finalRunNoChanges = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        initialResult.task(":generateJooqClasses").outcome == SUCCESS
        resultAfterChangeToExtension.task(":generateJooqClasses").outcome == SUCCESS
        finalRunNoChanges.task(":generateJooqClasses").outcome == UP_TO_DATE
    }

    def "up to date check works for generator customizations"() {
        given:
        def initialBuildGradle =
                """
                plugins {
                    id("dev.monosoul.jooq-docker")
                }
                
                repositories {
                    mavenCentral()
                }
                
                tasks {
                    generateJooqClasses {
                        schemas = arrayOf("public", "other")
                        customizeGenerator {
                            database.withExcludes("BAR")
                        }
                    }
                }
                
                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
                """
        def updatedBuildGradle =
                """
                plugins {
                    id("dev.monosoul.jooq-docker")
                }
                
                repositories {
                    mavenCentral()
                }
                
                tasks {
                    generateJooqClasses {
                        schemas = arrayOf("public", "other")
                        customizeGenerator {
                            database.withExcludes(".*")
                        }
                    }
                }
                
                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
                """
        copyResource("/V01__init_multiple_schemas.sql", "src/main/resources/db/migration/V01__init_multiple_schemas.sql")
        prepareBuildGradleFile(initialBuildGradle)

        when:
        def initialRun = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()
        prepareBuildGradleFile(updatedBuildGradle)
        def runAfterUpdate = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()
        def finalRunNoChanges = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        initialRun.task(":generateJooqClasses").outcome == SUCCESS
        runAfterUpdate.task(":generateJooqClasses").outcome == SUCCESS
        finalRunNoChanges.task(":generateJooqClasses").outcome == UP_TO_DATE
        def generatedPublic = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/public_/tables/Foo.java")
        def generatedOther = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
        !Files.exists(generatedPublic)
        !Files.exists(generatedOther)
    }

    def "generates jooq classes in a given package"() {
        given:
        prepareBuildGradleFile("""
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
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedClass = Paths.get(projectDir.getPath(), "build/generated-jooq/com/example/tables/Foo.java")
        Files.exists(generatedClass)
    }

    def "plugin is configurable"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      jooq {
                          image {
                              repository = "mysql"
                              tag = "8.0.15"
                              envVars = mapOf(
                                  "MYSQL_ROOT_PASSWORD" to "mysql",
                                  "MYSQL_DATABASE" to "mysql")
                              containerName = "uniqueMySqlContainerName"
                              readinessProbe = { host: String, port: Int ->
                                  arrayOf("sh", "-c", "until mysqladmin -h\$host -P\$port -uroot -pmysql ping; do echo wait; sleep 1; done;")
                              }
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
                      """)
        copyResource("/V01__init_mysql.sql", "src/main/resources/db/migration/V01__init_mysql.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses", "--stacktrace")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        Files.exists(generatedClass)
    }

    def "flyway configuration overridden with flywayProperties task input"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              flywayProperties = mapOf("flyway.placeholderReplacement" to "false")
                          }
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init_with_placeholders.sql", "src/main/resources/db/migration/V01__init_with_placeholders.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        Files.exists(generatedClass)
    }

    def "schema version provider is aware of flyway table name override"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              flywayProperties = mapOf("flyway.table" to "some_schema_table")
                          }
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedFlywayClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/SomeSchemaTable.java")
        Files.exists(generatedFlywayClass)
    }

    def "plugin works in Groovy gradle file"() {
        given:
        def buildGradleFile = new File(projectDir, "build.gradle")
        buildGradleFile.write(
                """
                      plugins {
                          id "dev.monosoul.jooq-docker"
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              flywayProperties = ["flyway.placeholderReplacement": "false"]
                              customizeGenerator {
                                  database.withExcludes("BAR")
                              }
                          }
                      }
                      
                      dependencies {
                          jdbc "org.postgresql:postgresql:42.3.6"
                      }
                      """)
        copyResource("/V01__init_with_placeholders.sql", "src/main/resources/db/migration/V01__init_with_placeholders.sql")
        copyResource("/V02__add_bar.sql", "src/main/resources/db/migration/V02__add_bar.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses", "--stacktrace")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedFoo = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def generatedBar = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Bar.java")
        Files.exists(generatedFoo)
        !Files.exists(generatedBar)
    }

    def "output schema to default properly passed to jOOQ generator"() {
        given:
        prepareBuildGradleFile("""
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
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedTableClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def generatedSchemaClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/DefaultSchema.java")
        Files.exists(generatedTableClass)
        Files.exists(generatedSchemaClass)
    }

    def "exclude flyway schema history"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              excludeFlywayTable = true
                          }
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def generatedFlywayClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/FlywaySchemaHistory.java")
        Files.exists(generatedFooClass)
        Files.notExists(generatedFlywayClass)
    }


    def "exclude flyway schema history given custom Flyway table name"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              excludeFlywayTable = true
                              flywayProperties = mapOf("flyway.table" to "some_schema_table")
                          }
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def generatedCustomFlywayClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/SomeSchemaTable.java")
        Files.exists(generatedFooClass)
        Files.notExists(generatedCustomFlywayClass)
    }


    def "exclude flyway schema history without overriding existing excludes"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              excludeFlywayTable = true
                              schemas = arrayOf("public", "other")
                              customizeGenerator {
                                  database.withExcludes("BAR")
                              }
                          }
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init_multiple_schemas.sql", "src/main/resources/db/migration/V01__init_multiple_schemas.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/public_/tables/Foo.java")
        def generatedBarClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
        def generatedFlywaySchemaClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/public_/tables/FlywaySchemaHistory.java")
        Files.exists(generatedFooClass)
        Files.notExists(generatedBarClass)
        Files.notExists(generatedFlywaySchemaClass)
    }

    def "outputDirectory task property is respected"() {
        given:
        prepareBuildGradleFile("""
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
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/gen/org/jooq/generated/tables/Foo.java")
        def generatedFlywayClass = Paths.get(projectDir.getPath(), "build/gen/org/jooq/generated/tables/FlywaySchemaHistory.java")
        Files.exists(generatedFooClass)
        Files.exists(generatedFlywayClass)
    }

    def "source sets and tasks are configured for java project"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          java
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                          implementation("org.jooq:jooq:3.16.6")
                          implementation("javax.annotation:javax.annotation-api:1.3.2")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")
        writeProjectFile("src/main/java/com/test/Main.java",
                """
                package com.test;
                
                import static org.jooq.generated.Tables.FOO;
                
                public class Main {
                    public static void main(String[] args) {
                        System.out.println(FOO.ID.getName());
                    }
                }
                """);

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("classes")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        result.task(":classes").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def mainClass = Paths.get(projectDir.getPath(), "build/classes/java/main/com/test/Main.class")
        Files.exists(generatedFooClass)
        Files.exists(mainClass)
    }

    def "source sets and tasks are configured for kotlin project"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          kotlin("jvm").version("1.3.50")
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      dependencies {
                          implementation(kotlin("stdlib"))
                          jdbc("org.postgresql:postgresql:42.3.6")
                          implementation("org.jooq:jooq:3.16.6")
                          implementation("javax.annotation:javax.annotation-api:1.3.2")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")
        writeProjectFile("src/main/kotlin/com/test/Main.kt",
                """
                package com.test
                
                import org.jooq.generated.Tables.FOO
                
                fun main() = println(FOO.ID.name)
                """);

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("classes")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        result.task(":classes").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def mainClass = Paths.get(projectDir.getPath(), "build/classes/kotlin/main/com/test/MainKt.class")
        Files.exists(generatedFooClass)
        Files.exists(mainClass)
    }

    def "source sets and tasks are configured for java project when jooq plugin is applied before java plugin"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      apply(plugin = "java")
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                          "implementation"("org.jooq:jooq:3.16.6")
                          "implementation"("javax.annotation:javax.annotation-api:1.3.2")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")
        writeProjectFile("src/main/java/com/test/Main.java",
                """
                package com.test;
                
                import static org.jooq.generated.Tables.FOO;
                
                public class Main {
                    public static void main(String[] args) {
                        System.out.println(FOO.ID.getName());
                    }
                }
                """);

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("classes")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        result.task(":classes").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def mainClass = Paths.get(projectDir.getPath(), "build/classes/java/main/com/test/Main.class")
        Files.exists(generatedFooClass)
        Files.exists(mainClass)
    }

    def "source sets and tasks are configured for java project when jooq plugin is applied before java-library plugin"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      apply(plugin = "java-library")
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                          "implementation"("org.jooq:jooq:3.16.6")
                          "implementation"("javax.annotation:javax.annotation-api:1.3.2")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")
        writeProjectFile("src/main/java/com/test/Main.java",
                """
                package com.test;
                
                import static org.jooq.generated.Tables.FOO;
                
                public class Main {
                    public static void main(String[] args) {
                        System.out.println(FOO.ID.getName());
                    }
                }
                """);

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("classes")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        result.task(":classes").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def mainClass = Paths.get(projectDir.getPath(), "build/classes/java/main/com/test/Main.class")
        Files.exists(generatedFooClass)
        Files.exists(mainClass)
    }

    def "source sets and tasks are configured for kotlin project when jooq plugin is applied before kotlin plugin"() {
        given:
        prepareBuildGradleFile("""
                      buildscript {
                          dependencies {
                              classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.50")
                          }
                      }
                      
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      apply(plugin = "org.jetbrains.kotlin.jvm")
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      dependencies {
                          "implementation"(kotlin("stdlib"))
                          jdbc("org.postgresql:postgresql:42.3.6")
                          "implementation"("org.jooq:jooq:3.16.6")
                          "implementation"("javax.annotation:javax.annotation-api:1.3.2")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")
        writeProjectFile("src/main/kotlin/com/test/Main.kt",
                """
                package com.test
                
                import org.jooq.generated.Tables.FOO
                
                fun main() = println(FOO.ID.name)
                """);

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("classes")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        result.task(":classes").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def mainClass = Paths.get(projectDir.getPath(), "build/classes/kotlin/main/com/test/MainKt.class")
        Files.exists(generatedFooClass)
        Files.exists(mainClass)
    }

    def "generateJooqClasses task output is loaded from cache"() {
        given:
        configureLocalGradleCache();
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        //first run loads to cache
        def firstRun = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses", "--build-cache")
                .build()
        //second run uses from cache
        new File(projectDir, 'build').deleteDir()
        def secondRun = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses", "--build-cache")
                .build()
        //third run got changes and can't use cached output
        new File(projectDir, 'build').deleteDir()
        copyResource("/V02__add_bar.sql", "src/main/resources/db/migration/V02__add_bar.sql")
        def thirdRun = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses", "--build-cache")
                .build()

        then:
        firstRun.task(":generateJooqClasses").outcome == SUCCESS
        secondRun.task(":generateJooqClasses").outcome == FROM_CACHE
        thirdRun.task(":generateJooqClasses").outcome == SUCCESS

        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def generatedFlywayClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/FlywaySchemaHistory.java")
        def generatedBarClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Bar.java")
        Files.exists(generatedFooClass)
        Files.exists(generatedBarClass)
        Files.exists(generatedFlywayClass)
    }

    def "regenerates jooq classes when out of date even though output directory already has classes generated"() {
        given:
        def initialBuildGradle =
                """
                import org.jooq.meta.jaxb.ForcedType
                
                plugins {
                    id("dev.monosoul.jooq-docker")
                }
                
                repositories {
                    mavenCentral()
                }
                
                tasks {
                    generateJooqClasses {
                        customizeGenerator {
                            database.withForcedTypes(ForcedType()
                                .withUserType("com.example.UniqueClassForFirstGeneration")
                                .withBinding("com.example.PostgresJSONGsonBinding")
                                .withTypes("JSONB"))
                        }
                    }
                }
                
                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
                """
        def updatedBuildFile =
                """
                import org.jooq.meta.jaxb.ForcedType
                
                plugins {
                    id("dev.monosoul.jooq-docker")
                }
                
                repositories {
                    mavenCentral()
                }
                
                tasks {
                    generateJooqClasses {
                        customizeGenerator {
                            database.withForcedTypes(ForcedType()
                                .withUserType("com.example.UniqueClassForSecondGeneration")
                                .withBinding("com.example.PostgresJSONGsonBinding")
                                .withTypes("JSONB"))
                        }
                    }
                }
                
                dependencies {
                    jdbc("org.postgresql:postgresql:42.3.6")
                }
                """
        prepareBuildGradleFile(initialBuildGradle)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def firstRun = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        firstRun.task(":generateJooqClasses").outcome == SUCCESS
        with(Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")) {
            Files.exists(it)
            Files.readAllLines(it).any { it.contains("com.example.UniqueClassForFirstGeneration") }
        }

        when:
        prepareBuildGradleFile(updatedBuildFile)
        def secondRun = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        secondRun.task(":generateJooqClasses").outcome == SUCCESS
        with(Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")) {
            Files.exists(it)
            Files.readAllLines(it).any { it.contains("com.example.UniqueClassForSecondGeneration") }
        }
    }

    def "generates flyway table in first schema by default"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              schemas = arrayOf("other", "public")
                          }
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init_multiple_schemas.sql", "src/main/resources/db/migration/V01__init_multiple_schemas.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedPublic = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/public_/tables/Foo.java")
        def generatedOther = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
        def generatedFlywaySchemaClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/other/tables/FlywaySchemaHistory.java")
        Files.exists(generatedPublic)
        Files.exists(generatedOther)
        Files.exists(generatedFlywaySchemaClass)
    }

    def "customizer has default generate object defined"() {
        given:
        prepareBuildGradleFile("""
                      plugins {
                          id("dev.monosoul.jooq-docker")
                      }
                      
                      repositories {
                          mavenCentral()
                      }
                      
                      tasks {
                          generateJooqClasses {
                              customizeGenerator {
                                  generate.setDeprecated(true)
                              }
                          }
                      }
                      
                      dependencies {
                          jdbc("org.postgresql:postgresql:42.3.6")
                      }
                      """)
        copyResource("/V01__init.sql", "src/main/resources/db/migration/V01__init.sql")

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("generateJooqClasses")
                .build()

        then:
        result.task(":generateJooqClasses").outcome == SUCCESS
        def generatedFooClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
        def generatedFlywayClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/FlywaySchemaHistory.java")
        Files.exists(generatedFooClass)
        Files.exists(generatedFlywayClass)
    }

    def configureLocalGradleCache() {
        def settingsGradleFile = new File(projectDir, "settings.gradle.kts")
        settingsGradleFile.write("""
                        buildCache {
                            local {
                                directory = "${localBuildCacheDirectory.path}"
                            }
                        }
                        """)
    }

    private void prepareBuildGradleFile(String script) {
        def buildGradleFile = new File(projectDir, "build.gradle.kts")
        buildGradleFile.write(script)
    }

    private void copyResource(String resource, String relativePath) {
        def file = new File(projectDir, relativePath)
        file.parentFile.mkdirs()
        getClass().getResourceAsStream(resource).transferTo(new FileOutputStream(file))
    }

    private void writeProjectFile(String relativePath, String content) {
        def file = new File(projectDir, relativePath)
        file.parentFile.mkdirs()
        file.write(content)
    }
}
