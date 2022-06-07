package dev.monosoul.jooq.util

import org.gradle.api.file.FileCollection
import java.net.URLClassLoader

internal class CodegenClasspathAwareClassLoaders(
    val buildscriptExclusive: URLClassLoader,
    val buildscriptInclusive: URLClassLoader,
) {
    companion object {
        fun from(classpath: FileCollection) = classpath.map {
            it.toURI().toURL()
        }.toTypedArray().let {
            CodegenClasspathAwareClassLoaders(
                buildscriptExclusive = URLClassLoader(it),
                buildscriptInclusive = URLClassLoader(it, CodegenClasspathAwareClassLoaders::class.java.classLoader)
            )
        }
    }
}
