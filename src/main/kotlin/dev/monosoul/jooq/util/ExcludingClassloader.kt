package dev.monosoul.jooq.util

internal class ExcludingClassloader(
    parent: ClassLoader,
    private val excludedClassPredicates: List<(String) -> Boolean>,
) : ClassLoader(parent) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (excludedClassPredicates.any { it(name) }) {
            throw ClassNotFoundException(name)
        }
        return super.loadClass(name, resolve)
    }
}
