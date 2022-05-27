package dev.monosoul.shaded.org.testcontainers.dockerclient.auth;

import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthConfigurations;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.core.SSLConfig;
import dev.monosoul.shaded.org.testcontainers.utility.DockerImageName;
import dev.monosoul.shaded.org.testcontainers.utility.RegistryAuthLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Generated;
import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * Facade implementation for {@link DockerClientConfig} which overrides how authentication
 * configuration is obtained. A delegate {@link DockerClientConfig} will be called first
 * to try and obtain auth credentials, but after that {@link RegistryAuthLocator} will be
 * used to try and improve the auth resolution (e.g. using credential helpers).
 *
 * @deprecated should not be used publicly, to be moved to docker-java
 */
@Deprecated
@Generated("https://github.com/testcontainers/testcontainers-java/blob/7d5f4c9e35b5d671f24125395aed3f741f6c3d9e/core/src/main/java/org/testcontainers/dockerclient/auth/AuthDelegatingDockerClientConfig.java")
public class AuthDelegatingDockerClientConfig implements DockerClientConfig {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DockerClientConfig delegate;

    public AuthDelegatingDockerClientConfig(DockerClientConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public URI getDockerHost() {
        return delegate.getDockerHost();
    }

    @Override
    public RemoteApiVersion getApiVersion() {
        return delegate.getApiVersion();
    }

    @Override
    public String getRegistryUsername() {
        return delegate.getRegistryUsername();
    }

    @Override
    public String getRegistryPassword() {
        return delegate.getRegistryPassword();
    }

    @Override
    public String getRegistryEmail() {
        return delegate.getRegistryEmail();
    }

    @Override
    public String getRegistryUrl() {
        return delegate.getRegistryUrl();
    }

    @Override
    public AuthConfig effectiveAuthConfig(String imageName) {
        // allow docker-java auth config to be used as a fallback
        AuthConfig fallbackAuthConfig;
        try {
            fallbackAuthConfig = delegate.effectiveAuthConfig(imageName);
        } catch (Exception e) {
            log.debug("Delegate call to effectiveAuthConfig failed with cause: '{}'. " +
                    "Resolution of auth config will continue using RegistryAuthLocator.",
                e.getMessage());
            fallbackAuthConfig = new AuthConfig();
        }

        // try and obtain more accurate auth config using our resolution
        final DockerImageName parsed = new DockerImageName(imageName);
        final AuthConfig effectiveAuthConfig = RegistryAuthLocator.instance()
            .lookupAuthConfig(parsed, fallbackAuthConfig);

        log.debug("Effective auth config [{}]", dev.monosoul.shaded.org.testcontainers.utility.AuthConfigUtil.toSafeString(effectiveAuthConfig));
        return effectiveAuthConfig;
    }

    @Override
    public AuthConfigurations getAuthConfigurations() {
        return delegate.getAuthConfigurations();
    }

    @Override
    public SSLConfig getSSLConfig() {
        return delegate.getSSLConfig();
    }
}
