package dev.monosoul.jooq.util

import java.util.Properties
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal class DependencyVersionsExtractor(
    resource: String = "/dev.monosoul.jooq.dependency.versions"
) : ReadOnlyProperty<Any?, String> {
    private val properties = javaClass.getResourceAsStream(resource)?.use {
        Properties().apply { load(it) }
    } ?: throw IllegalArgumentException("Dependency versions file not found: $resource")

    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        requireNotNull(properties.getProperty(property.name)) {
            "Dependency version for ${property.name} not found"
        }
}
