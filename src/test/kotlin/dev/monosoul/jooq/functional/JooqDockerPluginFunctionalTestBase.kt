package dev.monosoul.jooq.functional

import org.junit.jupiter.api.BeforeEach

abstract class JooqDockerPluginFunctionalTestBase : FunctionalTestBase() {

    @BeforeEach
    fun setUp() {
        copyResource("/testkit-gradle.properties", "gradle.properties")
    }

    override fun recreateProjectDir() {
        super.recreateProjectDir()
        setUp()
    }
}
