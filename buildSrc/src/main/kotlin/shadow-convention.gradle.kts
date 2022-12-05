import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import org.gradle.kotlin.dsl.kotlin

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

tasks {
    val relocateShadowJar by registering(ConfigureShadowRelocation::class) {
        target = shadowJar.get()
        prefix = "${project.group}.shadow"
    }

    shadowJar {
        mergeServiceFiles()

        fun inMetaInf(vararg patterns: String) = patterns.map { "META-INF/$it" }.toTypedArray()

        exclude(
            *inMetaInf("maven/**", "NOTICE*", "README*", "CHANGELOG*", "DEPENDENCIES*", "LICENSE*", "ABOUT*"),
            "LICENSE*",
        )

        dependsOn(relocateShadowJar)
    }
}
