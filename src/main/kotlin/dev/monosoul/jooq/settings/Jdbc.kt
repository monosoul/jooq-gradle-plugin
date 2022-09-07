package dev.monosoul.jooq.settings

import org.gradle.api.tasks.Input

data class Jdbc(
    @get:Input var schema: String = "jdbc:postgresql",
    @get:Input var driverClassName: String = "org.postgresql.Driver",
    @get:Input var urlQueryParams: String = "",
) : SettingsElement
