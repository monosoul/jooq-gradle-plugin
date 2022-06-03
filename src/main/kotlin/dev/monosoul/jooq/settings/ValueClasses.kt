package dev.monosoul.jooq.settings

import java.io.Serializable

internal data class JdbcDriverClassName(val value: String) : Serializable
internal data class JdbcUrl(val value: String) : Serializable
internal data class Username(val value: String) : Serializable
internal data class Password(val value: String) : Serializable
