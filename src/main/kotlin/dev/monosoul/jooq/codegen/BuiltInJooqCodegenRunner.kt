package dev.monosoul.jooq.codegen

import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration

internal class BuiltInJooqCodegenRunner(
    private val codegenAwareClassLoader: ClassLoader
) : JooqCodegenRunner {

    override fun generateJooqClasses(configuration: Configuration) {
        GenerationTool().apply {
            setClassLoader(codegenAwareClassLoader)
        }.run(configuration)
    }
}
