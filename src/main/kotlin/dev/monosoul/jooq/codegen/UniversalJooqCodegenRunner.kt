package dev.monosoul.jooq.codegen

import dev.monosoul.jooq.JooqDockerPlugin.Companion.CONFIGURATION_NAME
import dev.monosoul.jooq.util.CodegenClasspathAwareClassLoaders
import org.gradle.api.file.DirectoryProperty
import org.jooq.meta.jaxb.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class UniversalJooqCodegenRunner {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun generateJooqClasses(
        codegenAwareClassLoader: CodegenClasspathAwareClassLoaders,
        configuration: Configuration,
        outputDirectory: DirectoryProperty,
    ) {
        configuration.generator.target.directory = outputDirectory.asFile.get().toString()

        runCatching {
            ReflectiveJooqCodegenRunner(codegenAwareClassLoader.buildscriptExclusive)
        }.onFailure {
            logger.debug("Failed to load jOOQ code generation tool from $CONFIGURATION_NAME classpath", it)
        }.onSuccess {
            logger.info("Loaded jOOQ code generation tool from $CONFIGURATION_NAME classpath")
        }.getOrElse {
            logger.info("Loaded jOOQ code generation tool from buildscript classpath")
            BuiltInJooqCodegenRunner(codegenAwareClassLoader.buildscriptInclusive)
        }.generateJooqClasses(configuration)
    }
}
