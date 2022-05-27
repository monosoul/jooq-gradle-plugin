package dev.monosoul.jooq

import java.net.URI

class DatabaseHostResolver(private val dbHostOverride: String?) {

    @Throws(IllegalStateException::class)
    fun resolveHost(dockerHost: URI): String {
        return dbHostOverride ?: resolveFromDockerHost(dockerHost)
    }

    @Throws(IllegalStateException::class)
    private fun resolveFromDockerHost(dockerHost: URI): String {
        return when (dockerHost.scheme) {
            in arrayOf("http", "https", "tcp") -> dockerHost.host
            in arrayOf("unix", "npipe") -> "localhost"
            else -> throw IllegalStateException("could not resolve docker host for $dockerHost, " +
                    "please override it in plugin config \"jooq.db.hostOverride\"")
        }
    }
}
