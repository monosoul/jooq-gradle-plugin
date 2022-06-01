package dev.monosoul.shadowed.org.testcontainers.utility

import javax.annotation.Generated

@Generated("https://github.com/testcontainers/testcontainers-java/blob/de1324ed2800eff4da326d0c23d281399d006bc0/core/src/main/java/org/testcontainers/utility/Versioning.java")
internal sealed class Versioning {
    abstract fun isValid(): Boolean
    abstract fun getSeparator(): String

    object AnyVersion : Versioning() {
        override fun isValid() = true
        override fun getSeparator() = ":"

        override fun equals(other: Any?) = other is Versioning
        override fun toString() = "latest"
    }

    data class TagVersioning(private val tag: String) : Versioning() {
        companion object {
            private val TAG_REGEX = "[\\w][\\w.\\-]{0,127}".toRegex()
            val LATEST = TagVersioning("latest")
        }

        override fun isValid() = tag.matches(TAG_REGEX)
        override fun getSeparator() = ":"

        override fun toString() = tag
    }

    data class Sha256Versioning(private val hash: String) : Versioning() {
        companion object {
            private val HASH_REGEX = "[0-9a-fA-F]{32,}".toRegex()
        }

        override fun isValid() = hash.matches(HASH_REGEX)
        override fun getSeparator() = "@"

        override fun toString() = "sha256:$hash"
    }
}
