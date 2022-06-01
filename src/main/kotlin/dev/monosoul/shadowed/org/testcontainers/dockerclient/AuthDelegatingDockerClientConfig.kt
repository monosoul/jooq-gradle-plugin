package dev.monosoul.shadowed.org.testcontainers.dockerclient

import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.core.DockerClientConfig
import dev.monosoul.shadowed.org.testcontainers.utility.AuthConfigUtil.toSafeString
import dev.monosoul.shadowed.org.testcontainers.utility.DockerImageName
import dev.monosoul.shadowed.org.testcontainers.utility.RegistryAuthLocator
import org.slf4j.LoggerFactory
import javax.annotation.Generated

@Generated("https://github.com/testcontainers/testcontainers-java/blob/de1324ed2800eff4da326d0c23d281399d006bc0/core/src/main/java/org/testcontainers/dockerclient/AuthDelegatingDockerClientConfig.java")
internal class AuthDelegatingDockerClientConfig(
    private val delegate: DockerClientConfig
) : DockerClientConfig by delegate {

    private val log = LoggerFactory.getLogger(AuthDelegatingDockerClientConfig::class.java)

    override fun effectiveAuthConfig(imageName: String): AuthConfig? {
        // allow docker-java auth config to be used as a fallback
        val fallbackAuthConfig = try {
            delegate.effectiveAuthConfig(imageName)
        } catch (e: Exception) {
            log.debug(
                "Delegate call to effectiveAuthConfig failed with cause: '{}'. " +
                        "Resolution of auth config will continue using RegistryAuthLocator.",
                e.message
            )
            AuthConfig()
        }

        // try and obtain more accurate auth config using our resolution
        val parsed = DockerImageName.parse(imageName)
        val effectiveAuthConfig = RegistryAuthLocator
            .instance()
            .lookupAuthConfig(parsed, fallbackAuthConfig)

        log.debug("Effective auth config [{}]", toSafeString(effectiveAuthConfig))
        return effectiveAuthConfig
    }
}
