import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Paths

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class JooqDockerPluginSpec extends Specification {

    @TempDir
    File projectDir

    @TempDir
    File localBuildCacheDirectory

    def setup() {
        copyResource("testkit-gradle.properties", "gradle.properties")
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
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
                              generateUsingJavaConfig {
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
                    .build()

        then:
            result.task(":generateJooqClasses").outcome == SUCCESS
            def generatedPublic = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/public_/tables/Foo.java")
            def generatedOther = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
            Files.exists(generatedPublic)
            !Files.exists(generatedOther)
    }

    def "respects the generator customizations when using deprecated method"() {
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
                    .build()

        then:
            result.task(":generateJooqClasses").outcome == SUCCESS
            def generatedPublic = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/public_/tables/Foo.java")
            def generatedOther = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/other/tables/Bar.java")
            Files.exists(generatedPublic)
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
                    .build()

        then:
            result.task(":generateJooqClasses").outcome == SUCCESS
            def generatedClass = Paths.get(projectDir.getPath(), "build/generated-jooq/org/jooq/generated/tables/Foo.java")
            Files.exists(generatedClass)
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
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
                              generateUsingJavaConfig {
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
                    .build()

        then:
            result.task(":generateJooqClasses").outcome == SUCCESS
            def generatedFooClass = Paths.get(projectDir.getPath(), "build/gen/org/jooq/generated/tables/Foo.java")
            def generatedFlywayClass = Paths.get(projectDir.getPath(), "build/gen/org/jooq/generated/tables/FlywaySchemaHistory.java")
            Files.exists(generatedFooClass)
            Files.exists(generatedFlywayClass)
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--build-cache", "--stacktrace", "--debug")
                    .build()
            //second run uses from cache
            new File(projectDir, 'build').deleteDir()
            def secondRun = GradleRunner.create()
                    .withProjectDir(projectDir)
                    .withPluginClasspath()
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--build-cache", "--stacktrace", "--debug")
                    .build()
            //third run got changes and can't use cached output
            new File(projectDir, 'build').deleteDir()
            copyResource("/V02__add_bar.sql", "src/main/resources/db/migration/V02__add_bar.sql")
            def thirdRun = GradleRunner.create()
                    .withProjectDir(projectDir)
                    .withPluginClasspath()
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--build-cache", "--stacktrace", "--debug")
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
                              generateUsingJavaConfig {
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
                    .forwardOutput()
                    .withArguments("generateJooqClasses", "--stacktrace", "--debug")
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
