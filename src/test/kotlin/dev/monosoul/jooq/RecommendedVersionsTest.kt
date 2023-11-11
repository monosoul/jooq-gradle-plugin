package dev.monosoul.jooq

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isNotBlank
import strikt.assertions.isNotEqualTo

class RecommendedVersionsTest {
    @Test
    fun `should provide jooq version`() {
        expectThat(RecommendedVersions.JOOQ_VERSION).isNotBlank() isNotEqualTo "@jooq.version@"
    }

    @Test
    fun `should provide Flyway version`() {
        expectThat(RecommendedVersions.FLYWAY_VERSION).isNotBlank() isNotEqualTo "@flyway.version@"
    }
}
