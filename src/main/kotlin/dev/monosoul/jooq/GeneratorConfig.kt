package dev.monosoul.jooq

import org.jooq.meta.jaxb.Generator
import java.io.File
import java.io.Serializable

sealed class GeneratorConfig : Serializable {
    class FileBased(val configFile: File) : GeneratorConfig()
    class CodeBased(val generator: Generator) : GeneratorConfig()
}
