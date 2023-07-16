package dev.monosoul.jooq

import java.io.Serializable

/**
 * Sealed value class having a single private implementation.
 *
 * Provides a way to make some task input API stricter.
 */
sealed class ValueHolder<T> : Serializable
