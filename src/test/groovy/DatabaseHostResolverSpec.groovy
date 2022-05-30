import dev.monosoul.jooq.DatabaseHostResolver
import spock.lang.Specification

class DatabaseHostResolverSpec extends Specification {
    def "resolves database host based on docker host"() {
        given:
            def underSpec = new DatabaseHostResolver(null)

        when:
            def result = underSpec.resolveHost(dockerHost)

        then:
            result == expectedResult

        where:
            dockerHost                                || expectedResult
            new URI("http://hostname:6789")           || "hostname"
            new URI("https://fancyhost:6789")         || "fancyhost"
            new URI("tcp://another:6789")             || "another"
            new URI("unix:///var/run/docker.sock")    || "localhost"
            new URI("npipe:////./pipe/docker_engine") || "localhost"
    }

    def "throws IllegalStateException when unable to resolve database host from docker host"() {
        given:
            def underSpec = new DatabaseHostResolver(null)

        when:
            underSpec.resolveHost(host)

        then:
            thrown(IllegalStateException)

        where:
            host                      | _
            new URI("unknown://host") | _
            new URI("host")           | _
    }

    def "overrides database host when override provided"() {
        given:
            def dbHostOverride = "someoverride"
            def underSpec = new DatabaseHostResolver(dbHostOverride)

        when:
            def result = underSpec.resolveHost(new URI("http://localhost:8080"))

        then:
            result == dbHostOverride
    }

    def "does not throw exception when databe host cannot be resolved, but override was provided"() {
        given:
            def dbHostOverride = "someoverride"
            def underSpec = new DatabaseHostResolver(dbHostOverride)

        when:
            def result = underSpec.resolveHost(new URI("unknown://host"))

        then:
            result == dbHostOverride
    }
}
