rootProject.name = "testproject"

pluginManagement {
    repositories {
        maven {
            name = "localBuild"
            url = uri("./local-repository")
        }
        mavenCentral()
        gradlePluginPortal {
            content {
                excludeGroup("org.jooq")
                excludeGroup("org.flywaydb")
                excludeGroupByRegex("com\\.fasterxml.*")
                excludeGroupByRegex("com\\.google.*")
                excludeGroupByRegex("org\\.junit.*")
                excludeGroupByRegex("net\\.java.*")
                excludeGroupByRegex("jakarta.*")
            }
        }
    }
}
