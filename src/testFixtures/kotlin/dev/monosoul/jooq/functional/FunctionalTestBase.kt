package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import strikt.api.Assertion
import strikt.assertions.isNotNull
import java.io.File
import java.io.FileOutputStream

abstract class FunctionalTestBase {
    @TempDir
    private lateinit var projectDir: File

    protected open fun recreateProjectDir() {
        projectDir.recreate()
    }

    protected fun runGradleWithArguments(
        vararg arguments: String,
        projectDirectory: File = projectDir,
    ): BuildResult =
        GradleRunner
            .create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath()
            .forwardOutput()
            .withArguments(*arguments, "--stacktrace", "--info")
            .build()

    protected fun copyResource(
        from: String,
        to: String,
    ) = projectDir.copy(from, to)

    protected fun prepareBuildGradleFile(
        scriptName: String = "build.gradle.kts",
        scriptSupplier: () -> String,
    ) = projectDir.writeBuildGradleFile(scriptName, scriptSupplier)

    protected fun writeProjectFile(
        fileName: String,
        bodySupplier: () -> String,
    ) = projectDir.writeChild(fileName, bodySupplier)

    protected fun projectFile(fileName: String) = projectDir.getChild(fileName)

    protected fun Assertion.Builder<BuildResult>.getTask(taskName: String) =
        get { task(":$taskName") }
            .describedAs("Task $taskName")

    protected val Assertion.Builder<BuildTask?>.outcome get() = isNotNull().get { outcome }

    protected fun Assertion.Builder<BuildResult>.getTaskOutcome(taskName: String) = getTask(taskName).outcome

    protected val Assertion.Builder<BuildResult>.generateJooqClassesTask get() = getTask("generateJooqClasses")

    protected fun File.recreate() {
        deleteRecursively()
        mkdirs()
    }

    protected fun File.getChild(fileName: String) = File(this, fileName).also { it.parentFile.mkdirs() }

    protected fun File.writeChild(
        fileName: String,
        bodySupplier: () -> String,
    ) = getChild(fileName)
        .writeText(bodySupplier())

    protected fun File.writeBuildGradleFile(
        scriptName: String = "build.gradle.kts",
        scriptSupplier: () -> String,
    ) = writeChild(scriptName, scriptSupplier)

    protected fun File.copy(
        from: String,
        to: String,
    ) {
        val destinationFile = getChild(to)
        this@FunctionalTestBase.javaClass.getResourceAsStream(from)?.use { sourceStream ->
            FileOutputStream(destinationFile).use { destinationStream ->
                sourceStream.copyTo(destinationStream)
            }
        } ?: throw IllegalStateException("Resource not found: $from")
    }
}
