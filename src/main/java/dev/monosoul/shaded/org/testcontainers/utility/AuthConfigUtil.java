package dev.monosoul.shaded.org.testcontainers.utility;

import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Generated;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * TODO: Javadocs
 */
@Generated("https://github.com/testcontainers/testcontainers-java/blob/d127fd799bccbb4ee4d006dc2edd0f56c0c908c2/core/src/main/java/org/testcontainers/utility/AuthConfigUtil.java")
public class AuthConfigUtil {

    public static String toSafeString(AuthConfig authConfig) {
        if (authConfig == null) {
            return "null";
        }

        return MoreObjects.toStringHelper(authConfig)
            .add("username", authConfig.getUsername())
            .add("password", obfuscated(authConfig.getPassword()))
            .add("auth", obfuscated(authConfig.getAuth()))
            .add("email", authConfig.getEmail())
            .add("registryAddress", authConfig.getRegistryAddress())
            .add("registryToken", obfuscated(authConfig.getRegistrytoken()))
            .toString();
    }

    @NotNull
    private static String obfuscated(String value) {
        return isNullOrEmpty(value) ? "blank" : "hidden non-blank value";
    }
}
