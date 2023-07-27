This is how you can configure the plugin to use Java-based migrations from a submodule.

This is a preferred method of setting it up when your migrations are located in the same project,
because this way Gradle cache will work properly for `generateJooqClasses` task.
