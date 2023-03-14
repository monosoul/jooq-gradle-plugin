package dev.monosoul.jooq.util

import org.gradle.api.logging.Logger
import org.jooq.meta.jaxb.Logging

// covered by tests in artifact-tests module
fun Logger.getCodegenLogging(): Logging = when {
    isQuietEnabled && !isWarnEnabled -> Logging.ERROR
    isLifecycleEnabled && !isInfoEnabled -> Logging.ERROR
    isWarnEnabled && !isInfoEnabled -> Logging.WARN
    else -> Logging.DEBUG
}
