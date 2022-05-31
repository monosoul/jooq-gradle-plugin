package dev.monosoul.jooq.functional

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import strikt.api.Assertion
import strikt.assertions.isNotNull
import java.io.File
import java.io.FileOutputStream

abstract class JooqDockerPluginFunctionalTestBase {

    @TempDir
    private lateinit var projectDir: File

    @TempDir
    private lateinit var localBuildCacheDirectory: File

    @BeforeEach
    fun setUp() {
        copyResource("/testkit-gradle.properties", "gradle.properties")
    }

    protected fun runGradleWithArguments(vararg arguments: String): BuildResult = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .forwardOutput()
        .withArguments(*arguments, "--stacktrace", "--debug")
        .build()

    protected fun copyResource(from: String, to: String) {
        val destinationFile = projectFile(to)
        javaClass.getResourceAsStream(from)?.use { sourceStream ->
            FileOutputStream(destinationFile).use { destinationStream ->
                sourceStream.copyTo(destinationStream)
            }
        } ?: throw IllegalStateException("Resource not found: $from")
    }

    protected fun configureLocalGradleCache() {
        writeProjectFile("settings.gradle.kts") {
            """
                buildCache {
                    local {
                        directory = "${localBuildCacheDirectory.path}"
                    }
                }
            """.trimIndent()
        }
    }

    protected fun prepareBuildGradleFile(scriptName: String = "build.gradle.kts", scriptSupplier: () -> String) =
        writeProjectFile(scriptName, scriptSupplier)

    protected fun writeProjectFile(fileName: String, bodySupplier: () -> String) = projectFile(fileName)
        .writeText(bodySupplier())

    protected fun projectFile(fileName: String) = File(projectDir, fileName).also { it.parentFile.mkdirs() }

    protected fun Assertion.Builder<BuildResult>.getTask(taskName: String) = get { task(":$taskName") }
        .describedAs("Task $taskName")

    protected fun Assertion.Builder<BuildResult>.getTaskOutcome(taskName: String) =
        getTask(taskName).isNotNull().get { outcome }
}
