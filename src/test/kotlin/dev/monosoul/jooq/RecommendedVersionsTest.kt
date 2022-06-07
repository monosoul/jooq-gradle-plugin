package dev.monosoul.jooq

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isNotBlank

class RecommendedVersionsTest {

    @Test
    fun `should provide jooq version`() {
        expectThat(RecommendedVersions.JOOQ_VERSION).isNotBlank()
    }

    @Test
    fun `should provide Flyway version`() {
        expectThat(RecommendedVersions.FLYWAY_VERSION).isNotBlank()
    }
}
