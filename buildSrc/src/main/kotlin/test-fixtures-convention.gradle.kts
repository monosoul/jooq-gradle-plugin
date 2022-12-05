import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.`java-test-fixtures`

plugins {
    `java-test-fixtures`
}

/**
 * disable test fixtures publishing
 * https://docs.gradle.org/current/userguide/java_testing.html#publishing_test_fixtures
 */
val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
