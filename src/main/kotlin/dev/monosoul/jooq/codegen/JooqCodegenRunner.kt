package dev.monosoul.jooq.codegen

import org.jooq.meta.jaxb.Configuration

interface JooqCodegenRunner {
    fun generateJooqClasses(configuration: Configuration)
}
