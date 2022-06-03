package dev.monosoul.jooq.settings

import java.io.Serializable

class Jdbc(
    var schema: String = "jdbc:postgresql",
    var driverClassName: String = "org.postgresql.Driver",
    var urlQueryParams: String = "",
) : Serializable
