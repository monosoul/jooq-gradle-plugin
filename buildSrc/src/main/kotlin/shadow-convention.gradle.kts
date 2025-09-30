import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

shadow {
    addShadowVariantIntoJavaComponent = false
}

tasks {

    shadowJar {
        enableAutoRelocation = true
        relocationPrefix = "${project.group}.shadow"

        mergeServiceFiles()

        fun inMetaInf(vararg patterns: String) = patterns.map { "META-INF/$it" }.toTypedArray()

        exclude(
            *inMetaInf("maven/**", "NOTICE*", "README*", "CHANGELOG*", "DEPENDENCIES*", "LICENSE*", "ABOUT*"),
            "LICENSE*",
        )
    }
}
