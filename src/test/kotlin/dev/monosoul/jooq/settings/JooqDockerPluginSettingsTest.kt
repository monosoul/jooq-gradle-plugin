package dev.monosoul.jooq.settings

import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithContainer
import dev.monosoul.jooq.settings.JooqDockerPluginSettings.WithoutContainer
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class JooqDockerPluginSettingsTest {
    @Test
    fun `WithContainer should implement equals and hashcode`() {
        // given
        val withContainer1 = WithContainer()
        val withContainer2 = WithContainer().apply {
            database.apply {
                name = "dbname"
            }
            image.apply {
                name = "imagename"
            }
        }
        val withContainer3 = withContainer1.copy()

        // when & then
        expect {
            @Suppress("KotlinConstantConditions")
            that(withContainer1 == withContainer1).isTrue()
            that(withContainer1 == withContainer2).isFalse()
            that(withContainer1 == withContainer3).isTrue()

            that(withContainer1.hashCode() == withContainer1.hashCode()).isTrue()
            that(withContainer1.hashCode() == withContainer2.hashCode()).isFalse()
            that(withContainer1.hashCode() == withContainer3.hashCode()).isTrue()
        }
    }

    @Test
    fun `WithoutContainer should implement equals and hashcode`() {
        // given
        val withoutContainer1 = WithoutContainer()
        val withoutContainer2 = WithoutContainer().apply {
            database.apply {
                name = "dbname"
            }
        }
        val withoutContainer3 = withoutContainer1.copy()

        // when & then
        expect {
            @Suppress("KotlinConstantConditions")
            that(withoutContainer1 == withoutContainer1).isTrue()
            that(withoutContainer1 == withoutContainer2).isFalse()
            that(withoutContainer1 == withoutContainer3).isTrue()

            that(withoutContainer1.hashCode() == withoutContainer1.hashCode()).isTrue()
            that(withoutContainer1.hashCode() == withoutContainer2.hashCode()).isFalse()
            that(withoutContainer1.hashCode() == withoutContainer3.hashCode()).isTrue()
        }
    }
}
