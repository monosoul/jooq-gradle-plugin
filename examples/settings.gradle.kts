rootProject.name = "jooq-gradle-plugin-examples"

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
}



// Module declaration DSL, feel free to ignore it
data class IncludeTree(
    private val path: String,
    private val parentProject: String
) {
    fun dir(path: String, block: IncludeTree.() -> Unit) {
        val nestedPath = "${this.path}/$path"
        includeTree(nestedPath, parentProject, block)
    }

    fun module(name: String, block: IncludeTree.() -> Unit = {}) {
        val projectName = "$parentProject:$name"
        val projectDir = "$path/$name"

        include(projectName)
        project(projectName).also {
            it.projectDir = file(projectDir)

            it.buildFile.takeUnless(File::exists)
                ?.also { buildFile ->
                    buildFile.parentFile.mkdirs()
                    file("${buildFile.absolutePath}.kts").createNewFile()
                }
        }
        includeTree(projectDir, projectName, block)
    }
}

fun includeTree(path: String = ".", parentProject: String = "", block: IncludeTree.() -> Unit) {
    IncludeTree(path, parentProject).block()
}
