package dev.monosoul.jooq.util

import org.jooq.codegen.GenerationTool
import org.jooq.codegen.JavaGenerator
import org.jooq.meta.jaxb.Configuration
import java.io.InputStream

internal class JooqCodegenRunner {

    fun generateJooqClasses(jdbcAwareClassLoader: ClassLoader, configuration: Configuration) {
        val tool = GenerationTool()
        tool.setClassLoader(jdbcAwareClassLoader)
        tool.run(configuration)
    }

    companion object {
        val javaGeneratorName = JavaGenerator::class.qualifiedName
        fun load(inputStream: InputStream): Configuration = GenerationTool.load(inputStream)
    }
}
