package dev.monosoul.jooq.settings

import java.io.Serializable

sealed class Database : Serializable {
    abstract var username: String
    abstract var password: String
    abstract var name: String
    abstract var port: Int

    class Internal(
        override var username: String = "postgres",
        override var password: String = "postgres",
        override var name: String = "postgres",
        override var port: Int = 5432,
    ) : Database()

    class External(
        override var username: String = "postgres",
        override var password: String = "postgres",
        override var name: String = "postgres",
        var host: String = "localhost",
        override var port: Int = 5432,
    ) : Database()
}
