import com.github.benmanes.gradle.versions.updates.gradle.GradleReleaseChannel.CURRENT
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    groovy
    jacoco
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.14.0"
    id("pl.droidsonroids.jacoco.testkit") version "1.0.8"
    id("com.github.ben-manes.versions").version("0.38.0")
}

repositories {
    mavenCentral()
}

val targetJava = JavaVersion.VERSION_1_8
java {
    sourceCompatibility = targetJava
    targetCompatibility = targetJava
}

group = "com.revolut.jooq"
version = "0.3.7"

gradlePlugin {
    plugins.create("jooqDockerPlugin") {
        id = "com.revolut.jooq-docker"
        implementationClass = "com.revolut.jooq.JooqDockerPlugin"
        version = project.version
    }
}

pluginBundle {
    website = "https://github.com/revolut-engineering/jooq-plugin"
    vcsUrl = "https://github.com/revolut-engineering/jooq-plugin"

    description = "Generates jOOQ classes using dockerized database"

    (plugins) {
        "jooqDockerPlugin" {
            displayName = "jOOQ Docker Plugin"
            tags = listOf("jooq", "docker", "db")
            version = project.version.toString()
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

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "$targetJava"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(false)
        }
        setDependsOn(withType<Test>())
    }

    dependencyUpdates {
        resolutionStrategy {
            componentSelection {
                all {
                    val rejected = listOf("alpha", "beta", "b", "rc", "cr", "m", "preview")
                        .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]?.*") }
                        .any { it.matches(candidate.version) }
                    if (rejected) {
                        reject("Release candidate")
                    }
                }
            }
        }
        gradleReleaseChannel = CURRENT.id
    }
}

afterEvaluate {
    tasks.jacocoTestReport {
        classDirectories.setFrom(classDirectories.files.map {
            fileTree(it) {
                exclude("com/revolut/shaded/org/testcontainers/**/*")
            }
        })
    }
}


dependencies {
    implementation("org.jooq:jooq-codegen:3.14.8")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.3")
    implementation("com.github.docker-java:docker-java-transport-okhttp:3.2.8")
    implementation("org.flywaydb:flyway-core:6.4.3")
    implementation("org.zeroturnaround:zt-exec:1.12")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")

    testImplementation("org.spockframework:spock-core:2.1-groovy-3.0") {
        exclude("org.codehaus.groovy")
    }
    testCompileOnly(gradleTestKit())
}
