rootProject.name = "jooq-gradle-plugin"

include("extra-tests", "artifact-tests")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./libs.versions.toml"))
        }
    }
}
