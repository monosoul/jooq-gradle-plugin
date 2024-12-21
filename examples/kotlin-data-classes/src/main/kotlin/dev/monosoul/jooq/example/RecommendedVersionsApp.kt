package dev.monosoul.jooq.example

import org.jooq.generated.tables.records.FooRecord
import java.util.UUID


fun main() {
    println(FooRecord(UUID.randomUUID(), "someData"))
}
