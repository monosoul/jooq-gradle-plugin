import java.lang.Thread.sleep
import java.time.Duration

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

    withType<Test> {
        // workaround for https://github.com/gradle/gradle/issues/16603
        doLast {
            sleep(
                Duration.ofSeconds(2).toMillis()
            )
        }
    }
}
