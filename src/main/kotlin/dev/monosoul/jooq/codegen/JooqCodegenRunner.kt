package dev.monosoul.jooq.codegen

import org.jooq.meta.jaxb.Configuration

internal interface JooqCodegenRunner {
    fun generateJooqClasses(configuration: Configuration)
}
