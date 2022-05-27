package dev.monosoul.jooq

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig.newHostConfig
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.Ports.Binding.bindPort
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.core.command.ExecStartResultCallback
import com.github.dockerjava.okhttp.OkHttpDockerCmdExecFactory
import dev.monosoul.shaded.org.testcontainers.dockerclient.auth.AuthDelegatingDockerClientConfig
import org.gradle.api.Action
import java.io.Closeable
import java.lang.System.err
import java.lang.System.out
import java.util.UUID.randomUUID

class Docker(private val imageName: String,
             private val env: Map<String, Any>,
             private val portBinding: Pair<Int, Int>,
             private val readinessCommand: Array<String>,
             private val databaseHostResolver: DatabaseHostResolver,
             private val containerName: String = randomUUID().toString()) : Closeable {
    // https://github.com/docker-java/docker-java/issues/1048
    private val config: DockerClientConfig =
        AuthDelegatingDockerClientConfig(
            DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build()
        )
    private val docker: DockerClient = DockerClientImpl.getInstance(config)
            .withDockerCmdExecFactory(OkHttpDockerCmdExecFactory())

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
                .exec(ExecStartResultCallback(out, err))
                .awaitCompletion()
    }

    private fun resolveDbHost(): String {
        return databaseHostResolver.resolveHost(config.dockerHost)
    }

    private fun removeContainer() {
        try {
            docker.removeContainerCmd(containerName)
                    .withRemoveVolumes(true)
                    .withForce(true)
                    .exec()
        } catch (e: Exception) {
        }
    }

    override fun close() {
        docker.close()
    }
}
