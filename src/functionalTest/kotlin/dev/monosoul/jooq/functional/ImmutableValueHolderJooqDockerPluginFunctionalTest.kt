package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import strikt.api.expectThrows
import strikt.assertions.contains

class ImmutableValueHolderJooqDockerPluginFunctionalTest : JooqDockerPluginFunctionalTestBase() {
    @Test
    fun `should not be possible to create an instance of ValueHolder by extending it from Groovy`() {
        // given
        prepareBuildGradleFile("build.gradle") {
            // language=gradle
            """
            import dev.monosoul.jooq.ValueHolder
            
            plugins {
                id "dev.monosoul.jooq-docker"
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                jooqCodegen "org.postgresql:postgresql:42.3.6"
            }
            
            class InternalValueHolder<T> extends ValueHolder<T> {}
            
            new InternalValueHolder<String>()
            """.trimIndent()
        }

        // when & then
        expectThrows<UnexpectedBuildFailure> {
            runGradleWithArguments("tasks")
        }.get {
            stackTraceToString()
        }.contains(
            "The class 'InternalValueHolder' is not a permitted subclass of the sealed class 'dev.monosoul.jooq.ValueHolder'",
        )
    }
}
