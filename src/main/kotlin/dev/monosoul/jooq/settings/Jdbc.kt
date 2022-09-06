package dev.monosoul.jooq.settings

data class Jdbc(
    var schema: String = "jdbc:postgresql",
    var driverClassName: String = "org.postgresql.Driver",
    var urlQueryParams: String = "",
) : SettingsElement
