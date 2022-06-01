import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Paths

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class JooqDockerPluginSpec extends Specification {

    @TempDir
    File projectDir

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
