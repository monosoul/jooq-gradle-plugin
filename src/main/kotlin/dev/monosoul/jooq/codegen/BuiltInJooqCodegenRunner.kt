package dev.monosoul.jooq.codegen

import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration

internal class BuiltInJooqCodegenRunner(
    private val codegenAwareClassLoader: ClassLoader
) : JooqCodegenRunner {

    private val codeGenTool = GenerationTool().apply {
        setClassLoader(codegenAwareClassLoader)
    }

    override fun generateJooqClasses(configuration: Configuration) = codeGenTool.run(configuration)
}
