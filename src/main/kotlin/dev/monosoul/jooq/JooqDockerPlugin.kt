package dev.monosoul.jooq

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

open class JooqDockerPlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        configurations.create(CONFIGURATION_NAME)
        extensions.create<JooqExtension>("jooq")
        tasks.register<GenerateJooqClassesTask>("generateJooqClasses")
        pluginManager.withPlugin("org.gradle.java") {
            extensions.findByType<JavaPluginExtension>()?.run {
                sourceSets.named(MAIN_SOURCE_SET_NAME) {
                    java {
                        srcDirs(tasks.withType<GenerateJooqClassesTask>())
                    }
                }
            }
        }
    }

    internal companion object {
        const val CONFIGURATION_NAME = "jooqCodegen"
    }
}
