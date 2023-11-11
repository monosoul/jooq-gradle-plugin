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
        projectDir.deleteRecursively()
        projectDir.mkdirs()
    }

    protected fun runGradleWithArguments(vararg arguments: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .forwardOutput()
            .withArguments(*arguments, "--stacktrace", "--info")
            .build()

    protected fun copyResource(
        from: String,
        to: String,
    ) {
        val destinationFile = projectFile(to)
        javaClass.getResourceAsStream(from)?.use { sourceStream ->
            FileOutputStream(destinationFile).use { destinationStream ->
                sourceStream.copyTo(destinationStream)
            }
        } ?: throw IllegalStateException("Resource not found: $from")
    }

    protected fun prepareBuildGradleFile(
        scriptName: String = "build.gradle.kts",
        scriptSupplier: () -> String,
    ) = writeProjectFile(scriptName, scriptSupplier)

    protected fun writeProjectFile(
        fileName: String,
        bodySupplier: () -> String,
    ) = projectFile(fileName)
        .writeText(bodySupplier())

    protected fun projectFile(fileName: String) = File(projectDir, fileName).also { it.parentFile.mkdirs() }

    protected fun Assertion.Builder<BuildResult>.getTask(taskName: String) =
        get { task(":$taskName") }
            .describedAs("Task $taskName")

    protected val Assertion.Builder<BuildTask?>.outcome get() = isNotNull().get { outcome }

    protected fun Assertion.Builder<BuildResult>.getTaskOutcome(taskName: String) = getTask(taskName).outcome

    protected val Assertion.Builder<BuildResult>.generateJooqClassesTask get() = getTask("generateJooqClasses")
}
