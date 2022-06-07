package dev.monosoul.jooq.util

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.message

class DependencyVersionsExtractorTest {

    @Test
    fun `should throw exception if the specified resource does not exist`() {
        // when && then
        expectThrows<IllegalArgumentException> {
            DependencyVersionsExtractor("/does/not/exist")
        }.message isEqualTo "Dependency versions file not found: /does/not/exist"
    }

    @Test
    fun `should throw exception if the requested version does not exist`() {
        // given
        val extractor = DependencyVersionsExtractor("/DependencyVersionsExtractorTest/test.versions")

        // when
        val nonExistentVersion by extractor

        // then
        expectThrows<IllegalArgumentException> {
            nonExistentVersion
        }.message isEqualTo "Dependency version for nonExistentVersion not found"
    }

    @Test
    fun `should return existing version`() {
        // given
        val extractor = DependencyVersionsExtractor("/DependencyVersionsExtractorTest/test.versions")

        // when
        val existingVersion by extractor

        // then
        expectThat(existingVersion) isEqualTo "1.2.3"
    }
}
