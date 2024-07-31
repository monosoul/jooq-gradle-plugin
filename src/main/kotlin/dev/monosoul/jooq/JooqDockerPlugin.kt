package dev.monosoul.jooq

import dev.monosoul.jooq.settings.PropertiesReader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import javax.inject.Inject

open class JooqDockerPlugin
    @Inject
    constructor(
        private val providerFactory: ProviderFactory,
    ) : Plugin<Project> {
        override fun apply(project: Project) =
            with(project) {
                configurations.create(CONFIGURATION_NAME) {
                    attributes {
                        attribute(BUNDLING_ATTRIBUTE, objects.named(Bundling::class, Bundling.EXTERNAL))
                    }
                }
                extensions.create<JooqExtension>(
                    "jooq",
                    providerFactory.provider {
                        // TODO: this is a workaround for https://github.com/gradle/gradle/issues/21876
                        project.properties.entries
                            .filter { (key, _) ->
                                key.startsWith(PropertiesReader.PREFIX)
                            }.mapNotNull { (key, value) ->
                                (value as? String)?.let { key to it }
                            }.toMap()
                    },
                )
                tasks.register<GenerateJooqClassesTask>("generateJooqClasses")
                pluginManager.withPlugin("org.gradle.java") {
                    extensions.findByType<JavaPluginExtension>()?.run {
                        sourceSets.configureEach {
                            java {
                                srcDirs(
                                    tasks
                                        .withType<GenerateJooqClassesTask>()
                                        .matching { it.targetSourceSet.get() == this@configureEach.name },
                                )
                            }
                        }
                    }
                }
            }

        internal companion object {
            const val CONFIGURATION_NAME = "jooqCodegen"
        }
    }
