package dev.monosoul.shaded.org.testcontainers.utility

import com.github.dockerjava.api.model.AuthConfig
import com.google.common.base.MoreObjects

import javax.annotation.Generated

@Generated("https://github.com/testcontainers/testcontainers-java/blob/de1324ed2800eff4da326d0c23d281399d006bc0/core/src/main/java/org/testcontainers/utility/AuthConfigUtil.java")
internal object AuthConfigUtil {
    @JvmStatic
    fun toSafeString(authConfig: AuthConfig?) = authConfig?.let {
        MoreObjects
            .toStringHelper(it)
            .add("username", it.username)
            .add("password", obfuscated(it.password))
            .add("auth", obfuscated(it.auth))
            .add("email", it.email)
            .add("registryAddress", it.registryAddress)
            .add("registryToken", obfuscated(it.registrytoken))
            .toString()
    } ?: "null"

    @JvmStatic
    private fun obfuscated(value: String?) = if (value.isNullOrEmpty()) {
        "blank"
    } else {
        "hidden non-blank value"
    }
}
