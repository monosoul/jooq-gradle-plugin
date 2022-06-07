package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import strikt.api.expect
import strikt.assertions.isEqualTo
import strikt.java.exists
import kotlin.streams.asStream

class SourceSetsAndTasksConfigurationJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {

    @TestFactory
    fun `should configure source sets and tasks for java project`() = sequenceOf(
        "when jooq plugin is applied after java plugin" to """
            import dev.monosoul.jooq.RecommendedVersions
            
            plugins {
                java
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
                implementation("org.jooq:jooq:${'$'}{RecommendedVersions.JOOQ_VERSION}")
                implementation("javax.annotation:javax.annotation-api:1.3.2")
            }
        """.trimIndent(),
        "when jooq plugin is applied before java plugin" to """
            import dev.monosoul.jooq.RecommendedVersions
            
            plugins {
                id("dev.monosoul.jooq-docker")
            }
            apply(plugin = "java")

            repositories {
                mavenCentral()
            }

            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
                "implementation"("org.jooq:jooq:${'$'}{RecommendedVersions.JOOQ_VERSION}")
                "implementation"("javax.annotation:javax.annotation-api:1.3.2")
            }
        """.trimIndent(),
        "when jooq plugin is applied before java-library plugin" to """
            import dev.monosoul.jooq.RecommendedVersions
            
            plugins {
                id("dev.monosoul.jooq-docker")
            }
            apply(plugin = "java-library")

            repositories {
                mavenCentral()
            }

            dependencies {
                jooqCodegen("org.postgresql:postgresql:42.3.6")
                "implementation"("org.jooq:jooq:${'$'}{RecommendedVersions.JOOQ_VERSION}")
                "implementation"("javax.annotation:javax.annotation-api:1.3.2")
            }
        """.trimIndent()
    ).onEach {
        recreateProjectDir()
    }.map { (caseName, buildScript) ->
        dynamicTest("should configure source sets and tasks for java project $caseName") {
            // given
            prepareBuildGradleFile { buildScript }
            copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")
            writeProjectFile("src/main/java/com/test/Main.java") {
                // language=Java
                """
                    package com.test;

                    import static org.jooq.generated.Tables.FOO;

                    public class Main {
                        public static void main(String[] args) {
                            System.out.println(FOO.ID.getName());
                        }
                    }
                """.trimIndent()
            }

            // when
            val result = runGradleWithArguments("classes")

            // then
            expect {
                that(result).apply {
                    generateJooqClassesTask.outcome isEqualTo SUCCESS
                    getTaskOutcome("classes") isEqualTo SUCCESS
                }
                that(
                    projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
                ).exists()
                that(
                    projectFile("build/classes/java/main/com/test/Main.class")
                ).exists()
            }
        }
    }.asStream()

    @TestFactory
    fun `should configure source sets and tasks for kotlin project`() = sequenceOf(
        "when jooq plugin is applied after kotlin plugin" to """
            import dev.monosoul.jooq.RecommendedVersions
            
            plugins {
                kotlin("jvm") version "1.6.21"
                id("dev.monosoul.jooq-docker")
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation(kotlin("stdlib"))
                jooqCodegen("org.postgresql:postgresql:42.3.6")
                implementation("org.jooq:jooq:${'$'}{RecommendedVersions.JOOQ_VERSION}")
                implementation("javax.annotation:javax.annotation-api:1.3.2")
            }
        """.trimIndent(),
        "when jooq plugin is applied before kotlin plugin" to """
            import dev.monosoul.jooq.RecommendedVersions
            
            buildscript {
                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
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
                jooqCodegen("org.postgresql:postgresql:42.3.6")
                "implementation"("org.jooq:jooq:${'$'}{RecommendedVersions.JOOQ_VERSION}")
                "implementation"("javax.annotation:javax.annotation-api:1.3.2")
            }
        """.trimIndent()
    ).onEach {
        recreateProjectDir()
    }.map { (caseName, buildScript) ->
        dynamicTest("should configure source sets and tasks for kotlin project $caseName") {
            // given
            prepareBuildGradleFile { buildScript }
            copyResource(from = "/V01__init.sql", to = "src/main/resources/db/migration/V01__init.sql")
            writeProjectFile("src/main/kotlin/com/test/Main.kt") {
                // language=kotlin
                """
                    package com.test

                    import org.jooq.generated.Tables.FOO

                    fun main() = println(FOO.ID.name)
                """.trimIndent()
            }

            // when
            val result = runGradleWithArguments("classes")

            // then
            expect {
                that(result).apply {
                    generateJooqClassesTask.outcome isEqualTo SUCCESS
                    getTaskOutcome("classes") isEqualTo SUCCESS
                }
                that(
                    projectFile("build/generated-jooq/org/jooq/generated/tables/Foo.java")
                ).exists()
                that(
                    projectFile("build/classes/kotlin/main/com/test/MainKt.class")
                ).exists()
            }
        }
    }.asStream()
}
