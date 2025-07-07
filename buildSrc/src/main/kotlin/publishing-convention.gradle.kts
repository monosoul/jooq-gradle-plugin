import java.time.Duration

plugins {
    id("com.gradle.plugin-publish")
    signing
    id("io.github.gradle-nexus.publish-plugin")
}

val siteUrl = "https://github.com/monosoul/jooq-gradle-plugin"
val githubUrl = "https://github.com/monosoul/jooq-gradle-plugin"

val pluginName = "jOOQ Docker Plugin"
val pluginDescription = "Generates jOOQ classes using dockerized database"

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    val withSigning: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    setRequired({
        withSigning.toBoolean() && gradle.taskGraph.hasTask("publish")
    })
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
    transitionCheckOptions {
        maxRetries = 300
        delayBetween = Duration.ofSeconds(20)
    }
}

gradlePlugin {
    website.set(siteUrl)
    vcsUrl.set(githubUrl)

    plugins.create("jooqDockerPlugin") {
        id = "dev.monosoul.jooq-docker"
        implementationClass = "dev.monosoul.jooq.JooqDockerPlugin"
        version = project.version

        displayName = pluginName
        description = pluginDescription

        tags.set(listOf("jooq", "docker", "db"))
    }
}

publishing {
    publications {
        withType<MavenPublication>() {
            pom {
                name.set(pluginName)
                description.set(pluginDescription)
                url.set(siteUrl)
                scm {
                    url.set(githubUrl)
                    connection.set("scm:git:$githubUrl.git")
                    developerConnection.set("scm:git:$githubUrl.git")
                }
                developers {
                    developer {
                        id.set("monosoul")
                        name.set("Andrei Nevedomskii")
                    }
                }
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                issueManagement {
                    url.set("$githubUrl/issues")
                }
            }
        }
    }

    repositories {
        maven {
            name = "Snapshot"
            url = uri("https://maven.pkg.github.com/monosoul/jooq-gradle-plugin")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        val localRepositoryDirName by project.extra { "local-repository" }
        maven {
            name = "localBuild"
            url = uri("build/$localRepositoryDirName")
        }
    }
}
