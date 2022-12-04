package dev.monosoul.jooq.smoke

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import org.testcontainers.containers.BindMode.READ_ONLY
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.SelinuxContext.SHARED
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy
import org.testcontainers.utility.MountableFile

class GradleContainer : GenericContainer<GradleContainer>("gradle:7.6.0-jdk17-alpine") {

    private val listAppender = ListAppender<ILoggingEvent>().also { it.start() }
    private val logger = (LoggerFactory.getLogger("GradleContainer[$dockerImageName]") as Logger).also {
        it.addAppender(listAppender)
    }
    val output: List<String> get() = ArrayList(listAppender.list).map { it.formattedMessage }

    init {
        withLogConsumer(
            Slf4jLogConsumer(logger)
        )

        val projectPath = "/home/gradle/project"
        withCopyToContainer(MountableFile.forHostPath("build/resources/test/testproject"), projectPath)
        addFileSystemBind(
            "build/local-repository",
            "$projectPath/local-repository",
            READ_ONLY,
            SHARED
        )
        withWorkingDirectory(projectPath)

        withStartupCheckStrategy(
            IndefiniteWaitOneShotStartupCheckStrategy()
        )
    }
}
