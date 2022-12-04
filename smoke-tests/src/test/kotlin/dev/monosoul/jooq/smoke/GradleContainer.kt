package dev.monosoul.jooq.smoke

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.slf4j.LoggerFactory
import org.testcontainers.containers.BindMode.READ_ONLY
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.SelinuxContext.SHARED
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.utility.MountableFile
import java.util.Collections.synchronizedList

class GradleContainer : GenericContainer<GradleContainer>("gradle:7.6.0-jdk17-alpine") {

    private val rawOutput = synchronizedList(mutableListOf<ILoggingEvent>())
    private val listAppender = object : AppenderBase<ILoggingEvent>() {
        override fun append(eventObject: ILoggingEvent) {
            rawOutput.add(eventObject)
        }
    }.also { it.start() }
    private val logger = (LoggerFactory.getLogger("GradleContainer[$dockerImageName]") as Logger).also {
        it.addAppender(listAppender)
    }
    val output: List<String> get() = rawOutput.map { it.formattedMessage }

    init {
        withLogConsumer(
            Slf4jLogConsumer(logger)
        )

        val projectPath = "/home/gradle/project"
        withCopyToContainer(MountableFile.forHostPath("build/resources/test/testproject"), projectPath)
        addFileSystemBind(
            "build/libs/plugin.jar",
            "$projectPath/plugin/plugin.jar",
            READ_ONLY,
            SHARED
        )
        withWorkingDirectory(projectPath)

        waitingFor(
            LogMessageWaitStrategy()
                .withRegEx(".*(BUILD SUCCESSFUL|BUILD FAILED) in .*")
        )
    }
}
