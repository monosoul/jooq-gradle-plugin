import dev.monosoul.gradle.module.tree.includeTree

rootProject.name = "jooq-gradle-plugin-examples"

plugins {
    id("dev.monosoul.module-tree") version "0.0.2"
}

includeTree {
    dir("spring-boot") {
        module("recommended-jooq-version")
        module("old-jooq-version")
        module("newer-jooq-version")
    }
    module("mysql")
    module("multiple-databases")
    module("external-database")
    module("properties")
    module("java-codegen-configuration")
    module("xml-codegen-configuration")
    module("recommended-jooq-flyway-versions")
    dir("java-based-migrations") {
        module("submodule-classes") {
            module("migrations")
        }
        module("submodule-dependency-configuration") {
            module("migrations")
        }
        module("third-party-jar")
    }
    module("kotlin-data-classes")
}
