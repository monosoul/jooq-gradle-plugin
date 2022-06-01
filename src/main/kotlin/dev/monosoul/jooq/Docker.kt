package dev.monosoul.jooq

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig.newHostConfig
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.Ports.Binding.bindPort
import com.github.dockerjava.api.model.StreamType.RAW
import com.github.dockerjava.api.model.StreamType.STDERR
import com.github.dockerjava.api.model.StreamType.STDOUT
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.okhttp.OkDockerHttpClient
import dev.monosoul.jooq.DockerCommandLogger.Companion.logger
import dev.monosoul.shadowed.org.testcontainers.dockerclient.AuthDelegatingDockerClientConfig
import org.gradle.api.Action
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.UUID.randomUUID

class Docker(
    private val imageName: String,
    private val env: Map<String, Any>,
    private val portBinding: Pair<Int, Int>,
    private val readinessCommand: Array<String>,
    private val databaseHostResolver: DatabaseHostResolver,
    private val containerName: String = randomUUID().toString()
) : Closeable {
    // https://github.com/docker-java/docker-java/issues/1048
    private val config: DockerClientConfig = AuthDelegatingDockerClientConfig(
        DefaultDockerClientConfig
            .createDefaultConfigBuilder()
            .build()
    )
    private val docker: DockerClient = DockerClientImpl.getInstance(
        config,
        OkDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .sslConfig(config.sslConfig)
            .build()
    )

    fun runInContainer(action: Action<String>) {
        try {
            val dbHost = resolveDbHost()
            removeContainer()
            prepareDockerizedDb()
            action.execute(dbHost)
        } finally {
            removeContainer()
        }
    }

    private fun prepareDockerizedDb() {
        pullImage()
        startContainer()
        awaitContainerStart()
    }

    private fun pullImage() {
        val callback = PullImageResultCallback()
        docker.pullImageCmd(imageName).exec(callback)
        callback.awaitCompletion()
    }

    private fun startContainer() {
        val dbPort = ExposedPort.tcp(portBinding.first)
        docker.createContainerCmd(imageName)
            .withName(containerName)
            .withEnv(env.map { "${it.key}=${it.value}" })
            .withExposedPorts(dbPort)
            .withHostConfig(newHostConfig().withPortBindings(Ports(dbPort, bindPort(portBinding.second))))
            .exec()
        docker.startContainerCmd(containerName).exec()
    }

    private fun awaitContainerStart() {
        val execCreate = docker.execCreateCmd(containerName)
            .withCmd(*readinessCommand)
            .withAttachStdout(true)
            .exec()
        docker.execStartCmd(execCreate.id)
            .exec(DockerCommandLogger())
            .awaitCompletion()
    }

    private fun resolveDbHost(): String {
        return databaseHostResolver.resolveHost(config.dockerHost)
    }

    private fun removeContainer() {
        runCatching {
            docker.removeContainerCmd(containerName)
                .withRemoveVolumes(true)
                .withForce(true)
                .exec()
        }.onFailure {
            logger.debug("Failed to remove container", it)
        }
    }

    override fun close() {
        docker.close()
    }
}

private class DockerCommandLogger : ResultCallback.Adapter<Frame>() {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(Docker::class.java)
    }

    override fun onNext(frame: Frame?) {
        frame?.run {
            when (streamType) {
                STDOUT -> logger.info(payload.decodeToString())
                STDERR -> logger.error(payload.decodeToString())
                RAW -> logger.info(payload.decodeToString())
                else -> logger.error("unknown stream type: $streamType")
            }
        }
    }
}
