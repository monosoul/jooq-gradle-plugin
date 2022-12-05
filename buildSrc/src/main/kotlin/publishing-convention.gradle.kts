plugins {
    id("com.gradle.plugin-publish")
}

gradlePlugin {
    plugins.create("jooqDockerPlugin") {
        id = "dev.monosoul.jooq-docker"
        implementationClass = "dev.monosoul.jooq.JooqDockerPlugin"
        version = project.version

        displayName = "jOOQ Docker Plugin"
        description = "Generates jOOQ classes using dockerized database"
    }
}

pluginBundle {
    website = "https://github.com/monosoul/jooq-gradle-plugin"
    vcsUrl = "https://github.com/monosoul/jooq-gradle-plugin"

    pluginTags = mapOf(
        "jooqDockerPlugin" to listOf("jooq", "docker", "db"),
    )
}

publishing {
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
