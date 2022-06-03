package dev.monosoul.jooq.settings

import java.io.Serializable

class Jdbc : Serializable {
    var schema = "jdbc:postgresql"
    var driverClassName = "org.postgresql.Driver"
    var urlQueryParams = ""
}
