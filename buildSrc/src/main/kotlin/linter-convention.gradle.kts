import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.EXTERNAL

plugins {
    kotlin("jvm")
}

val Project.libs: VersionCatalog
    get() = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(libs.findLibrary("ktlint").get()) {
        attributes {
            attributes {
                attribute(BUNDLING_ATTRIBUTE, getObjects().named(Bundling::class, EXTERNAL))
            }
        }
    }
}

tasks {
    val sourceInputFiles = fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))

    val ktlintCheckSources by registering(JavaExec::class) {
        inputs.files(sourceInputFiles)

        description = "Check Kotlin code style."
        group = "ktlint"
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        args("--color", "src/**/*.kt")
    }

    val ktlintFormatSources by registering(JavaExec::class) {
        inputs.files(sourceInputFiles)

        description = "Fix Kotlin code style deviations."
        group = "ktlint"
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        args("--color", "-F", "src/**/*.kt")
    }

    val buildscriptInputFiles = fileTree(mapOf("dir" to ".", "include" to "*.gradle.kts"))

    val ktlintCheckBuildScript by registering(JavaExec::class) {
        inputs.files(buildscriptInputFiles)

        description = "Check buildscript code style."
        group = "ktlint"
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        args("--color", "*.gradle.kts")
    }

    val ktlintFormatBuildScript by registering(JavaExec::class) {
        inputs.files(buildscriptInputFiles)

        description = "Fix buildscript code style deviations."
        group = "ktlint"
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        args("--color", "-F", "*.gradle.kts")
    }

    val ktlintCheckAll by registering {
        dependsOn(ktlintCheckBuildScript, ktlintCheckSources)

        description = "Check all files code style."
        group = "ktlint"
    }

    register("ktlintFormatAll") {
        dependsOn(ktlintFormatBuildScript, ktlintFormatSources)

        description = "Fix all code style deviations."
        group = "ktlint"
    }

    check {
        dependsOn(ktlintCheckAll)
    }

    withType<Test> {
        shouldRunAfter(ktlintCheckBuildScript, ktlintCheckSources)
    }
}
