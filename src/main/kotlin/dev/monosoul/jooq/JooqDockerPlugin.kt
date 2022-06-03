package dev.monosoul.jooq

import org.gradle.api.Plugin
import org.gradle.api.Project

open class JooqDockerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.configurations.create("jdbc")
        project.extensions.create("jooq", JooqExtension::class.java)
        project.tasks.create("generateJooqClasses", GenerateJooqClassesTask::class.java) {
            group = "jooq"
        }
    }
}
