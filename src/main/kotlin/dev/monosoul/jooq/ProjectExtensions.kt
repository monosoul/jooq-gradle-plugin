package dev.monosoul.jooq

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.net.URLClassLoader

internal fun Project.jdbcAwareClassloaderProvider(): Provider<URLClassLoader> = configurations.named("jdbc")
    .map { configuration ->
        configuration.resolvedConfiguration.resolvedArtifacts.map { artifact ->
            artifact.file.toURI().toURL()
        }.toTypedArray().let { resolvedJdbcArtifacts ->
            URLClassLoader(resolvedJdbcArtifacts, buildscript.classLoader)
        }
    }
