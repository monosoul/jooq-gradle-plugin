This is how you can configure the plugin to use Java-based migrations from a submodule.

While this method allows you to pull in some extra dependencies into the Flyway classpath along with your migrations, it
doesn't work well with Gradle cache. The reason for that is that the codegen task depends now on JAR artifact of
migrations submodule, and Gradle task to build JAR doesn't cache its outputs.
