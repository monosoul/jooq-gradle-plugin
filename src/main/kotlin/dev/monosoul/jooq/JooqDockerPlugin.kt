package dev.monosoul.jooq

import dev.monosoul.jooq.settings.PropertiesReader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import javax.inject.Inject

open class JooqDockerPlugin @Inject constructor(
    private val providerFactory: ProviderFactory
) : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        configurations.create(CONFIGURATION_NAME)
        extensions.create<JooqExtension>("jooq", providerFactory.provider {
            // TODO: this is a workaround for https://github.com/gradle/gradle/issues/21876
            project.properties.entries.filter { (key, _) ->
                key.startsWith(PropertiesReader.PREFIX)
            }.mapNotNull { (key, value) ->
                (value as? String)?.let { key to it }
            }.toMap()
        })
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JooqDockerPlugin

        if (providerFactory != other.providerFactory) return false

        return true
    }

    override fun hashCode(): Int {
        return providerFactory.hashCode()
    }


    internal companion object {
        const val CONFIGURATION_NAME = "jooqCodegen"
    }
}
