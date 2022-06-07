package dev.monosoul.jooq

import dev.monosoul.jooq.util.ExcludingClassloader
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.net.URLClassLoader

internal fun Project.jdbcAwareClassloaderProvider(): Provider<URLClassLoader> = configurations
    .named(JooqDockerPlugin.CONFIGURATION_NAME)
    .map { configuration ->
        configuration.resolvedConfiguration.resolvedArtifacts.map { artifact ->
            artifact.file.toURI().toURL()
        }.toTypedArray().let { resolvedJdbcArtifacts ->
            URLClassLoader(resolvedJdbcArtifacts, buildscript.classLoader)
        }
    }

internal fun Project.codegenClasspathAwareClassloaderProvider(): Provider<Classloaders> = configurations
    .named(JooqDockerPlugin.CONFIGURATION_NAME_2)
    .map { configuration ->
        configuration.resolvedConfiguration.resolvedArtifacts.map { artifact ->
            artifact.file.toURI().toURL()
        }.toTypedArray().let { resolvedJdbcArtifacts ->
            Classloaders(
                buildscriptExclusive = URLClassLoader(
                    resolvedJdbcArtifacts,
                    ExcludingClassloader(
                        buildscript.classLoader,
                        listOf(
                            { it.startsWith("org.jooq") },
                            // { it.startsWith("org.flywaydb") }
                        )
                    )
                ),
                buildscriptInclusive = URLClassLoader(resolvedJdbcArtifacts, buildscript.classLoader)
            )
        }
    }

internal class Classloaders(
    val buildscriptExclusive: URLClassLoader,
    val buildscriptInclusive: URLClassLoader,
)
