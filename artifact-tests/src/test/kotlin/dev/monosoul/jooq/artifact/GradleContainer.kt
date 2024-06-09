package dev.monosoul.jooq.artifact

import org.slf4j.LoggerFactory
import org.testcontainers.containers.BindMode.READ_ONLY
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.SelinuxContext.SHARED
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.output.ToStringConsumer
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy

class GradleContainer(
    dockerSocketPath: String = "/var/run/docker.sock",
) : GenericContainer<GradleContainer>("gradle:8.6.0-jdk17-alpine") {
    private val toStringLogConsumer = ToStringConsumer()
    val output: String get() = toStringLogConsumer.toUtf8String()
    val projectPath = "/home/gradle/project"

    init {
        withLogConsumer(
            Slf4jLogConsumer(LoggerFactory.getLogger("GradleContainer[$dockerImageName]")),
        )
        withLogConsumer(toStringLogConsumer)
        addFsBind("build/local-repository", "$projectPath/local-repository")
        addFsBind("/var/run/docker.sock", dockerSocketPath)
        withWorkingDirectory(projectPath)

        withStartupCheckStrategy(
            IndefiniteWaitOneShotStartupCheckStrategy(),
        )
    }

    private fun addFsBind(
        hostPath: String,
        containerPath: String,
    ) = addFileSystemBind(hostPath, containerPath, READ_ONLY, SHARED)
}
