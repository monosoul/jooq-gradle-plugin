package dev.monosoul.jooq.settings

import java.io.Serializable

internal interface SettingsElement : Serializable {
    override fun hashCode(): Int

    override fun equals(other: Any?): Boolean
}
