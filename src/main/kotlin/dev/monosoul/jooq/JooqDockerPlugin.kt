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

    override fun apply(project: Project) {
        project.configurations.create(CONFIGURATION_NAME)
        project.extensions.create<JooqExtension>("jooq")
        project.tasks.register<GenerateJooqClassesTask>("generateJooqClasses")
        project.extensions.findByType<JavaPluginExtension>()?.run {
            sourceSets.named(MAIN_SOURCE_SET_NAME) {
                java {
                    srcDirs(project.tasks.withType<GenerateJooqClassesTask>())
                }
            }
        }
    }

    internal companion object {
        const val CONFIGURATION_NAME = "jooqCodegen"
    }
}
