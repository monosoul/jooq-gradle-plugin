import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

val targetJava = JavaVersion.VERSION_1_8
java {
    sourceCompatibility = targetJava
    targetCompatibility = targetJava
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("$targetJava")
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        named<JvmTestSuite>("test") {
            useJUnitJupiter()
        }
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(STARTED, PASSED, FAILED)
            showExceptions = true
            showStackTraces = true
            showCauses = true
            exceptionFormat = FULL
        }
    }
}

val processTemplates by tasks.registering(Copy::class) {
    from("src/main/template/kotlin")
    into("build/filtered-templates/kotlin/main")
}

val processTestTemplates by tasks.registering(Copy::class) {
    from("src/test/template/kotlin")
    into("build/filtered-templates/kotlin/test")
}

sourceSets {
    main {
        java {
            srcDir(processTemplates)
        }
    }
    test {
        java {
            srcDir(processTestTemplates)
        }
    }
}
