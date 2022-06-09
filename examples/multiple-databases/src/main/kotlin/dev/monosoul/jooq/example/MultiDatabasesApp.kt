package dev.monosoul.jooq.example

import org.jooq.generated.mysql.tables.records.FooRecord as MySqlFooRecord
import org.jooq.generated.postgres.tables.records.FooRecord as PostgresFooRecord


fun main() {
    println(PostgresFooRecord())
    println(MySqlFooRecord())
}
