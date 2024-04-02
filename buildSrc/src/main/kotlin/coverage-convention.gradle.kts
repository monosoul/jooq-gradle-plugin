plugins {
    kotlin("jvm")
    jacoco
    id("pl.droidsonroids.jacoco.testkit")
}

tasks {
    val testTasks = tasks.withType<Test>()
    jacocoTestReport {
        executionData.setFrom(
            testTasks.map { it.extensions.getByType<JacocoTaskExtension>().destinationFile }
        )

        reports {
            xml.required.set(true)
            html.required.set(false)
        }
        shouldRunAfter(testTasks)
    }
}
