package dev.monosoul.jooq.codegen

import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import org.jooq.util.jaxb.tools.MiniJAXB
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.reflect.jvm.jvmName

internal class ReflectiveJooqCodegenRunner(
    codegenAwareClassLoader: ClassLoader
) : JooqCodegenRunner {

    private val codeGenTool = ReflectiveGenerationTool(codegenAwareClassLoader).apply {
        setClassLoader(codegenAwareClassLoader)
    }

    override fun generateJooqClasses(configuration: Configuration) = codeGenTool.run(configuration)

    private class ReflectiveGenerationTool(codegenAwareClassLoader: ClassLoader) {
        private val toolClass = codegenAwareClassLoader.loadClass(GenerationTool::class.jvmName)
        private val configurationClass = codegenAwareClassLoader.loadClass(Configuration::class.jvmName)
        private val tool = toolClass.getDeclaredConstructor().newInstance()

        fun setClassLoader(classLoader: ClassLoader) {
            val setClassLoaderMethod = toolClass.getDeclaredMethod(
                GenerationTool::setClassLoader.name,
                ClassLoader::class.java
            )
            setClassLoaderMethod.invoke(tool, classLoader)
        }

        fun run(configuration: Configuration) {
            val preparedConfiguration = load(configuration.toXmlByteArray())
            val runMethod = toolClass.getMethod(GenerationTool::run.name, configurationClass)
            runMethod.invoke(tool, preparedConfiguration)
        }

        private fun Configuration.toXmlByteArray() = ByteArrayOutputStream().also { stream ->
            stream.writer().use { writer ->
                MiniJAXB.marshal(this, writer)
                writer.flush()
            }
        }.toByteArray()

        private fun load(xmlByteArray: ByteArray): Any {
            val loadMethod = toolClass.getMethod(GenerationTool::load.name, InputStream::class.java)
            return ByteArrayInputStream(xmlByteArray).use {
                loadMethod.invoke(null, it)
            }
        }
    }
}
