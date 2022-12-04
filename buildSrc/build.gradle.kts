plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
}

gradlePlugin {
    plugins {
        register("kotlin-convention") {
            id = "kotlin-convention"
            implementationClass = "dev.monosoul.jooq.buildscript.KotlinConventionPlugin"
        }
    }
}
