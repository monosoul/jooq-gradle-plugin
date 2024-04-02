rootProject.name = "jooq-gradle-plugin"

include("artifact-tests")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./libs.versions.toml"))
        }
    }
}
