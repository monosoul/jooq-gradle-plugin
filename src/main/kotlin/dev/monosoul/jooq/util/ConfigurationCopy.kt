package dev.monosoul.jooq.util

import org.jooq.meta.jaxb.Configuration
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

internal fun Configuration.copy(): Configuration {
    val serialized =
        ByteArrayOutputStream().apply {
            ObjectOutputStream(this).use { oos ->
                oos.writeObject(this@copy)
            }
        }.toByteArray()

    return ObjectInputStream(ByteArrayInputStream(serialized)).use { ois ->
        ois.readObject() as Configuration
    }
}
