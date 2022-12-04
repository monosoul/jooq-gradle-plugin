package dev.monosoul.jooq.buildscript

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class KotlinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.repositories {
            mavenCentral()
        }

        target.pluginManager.withPlugin("org.gradle.kotlin.kotlin-dsl") {
            val targetJava = JavaVersion.VERSION_1_8

            target.extensions.configure<JavaPluginExtension> {
                sourceCompatibility = targetJava
                targetCompatibility = targetJava
            }

            target.tasks {
                withType<Test> {
                    useJUnitPlatform()
                    testLogging {
                        events(STARTED, PASSED, FAILED)
                        showExceptions = true
                        showStackTraces = true
                        showCauses = true
                        exceptionFormat = FULL
                    }
                }

                withType<KotlinCompile> {
                    kotlinOptions {
                        jvmTarget = "$targetJava"
                        freeCompilerArgs = listOf("-Xjsr305=strict")
                    }
                }
            }
        }
    }
}
