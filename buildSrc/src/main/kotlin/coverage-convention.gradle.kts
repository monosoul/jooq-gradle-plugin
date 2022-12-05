plugins {
    kotlin("jvm")
    jacoco
    id("pl.droidsonroids.jacoco.testkit")
}

tasks {
    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(false)
        }
        dependsOn(withType<Test>())
    }
}
