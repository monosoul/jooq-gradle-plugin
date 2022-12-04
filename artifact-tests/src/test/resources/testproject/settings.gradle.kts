rootProject.name = "testproject"

pluginManagement {
    repositories {
        maven {
            name = "localBuild"
            url = uri("./local-repository")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
