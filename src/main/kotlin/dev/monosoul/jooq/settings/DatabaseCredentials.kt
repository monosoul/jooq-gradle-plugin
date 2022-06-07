package dev.monosoul.jooq.settings

internal data class DatabaseCredentials(
    val jdbcDriverClassName: String,
    val jdbcUrl: String,
    val username: String,
    val password: String,
)
