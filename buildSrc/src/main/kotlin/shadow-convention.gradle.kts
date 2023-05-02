plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

tasks {

    shadowJar {
        isEnableRelocation = true
        relocationPrefix = "${project.group}.shadow"

        mergeServiceFiles()

        fun inMetaInf(vararg patterns: String) = patterns.map { "META-INF/$it" }.toTypedArray()

        exclude(
            *inMetaInf("maven/**", "NOTICE*", "README*", "CHANGELOG*", "DEPENDENCIES*", "LICENSE*", "ABOUT*"),
            "LICENSE*",
        )
    }
}
