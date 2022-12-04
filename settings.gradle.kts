rootProject.name = "jooq-gradle-plugin"

include("extra-tests", "smoke-tests")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./libs.versions.toml"))
        }
    }
}
