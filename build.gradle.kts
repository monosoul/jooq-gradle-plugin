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
    id("com.gradle.plugin-publish") version "0.21.0"
    id("pl.droidsonroids.jacoco.testkit") version "1.0.9"
}

repositories {
    mavenCentral()
}

val targetJava = JavaVersion.VERSION_1_8
java {
    sourceCompatibility = targetJava
    targetCompatibility = targetJava
}

group = "dev.monosoul.jooq"

gradlePlugin {
    plugins.create("jooqDockerPlugin") {
        id = "dev.monosoul.jooq-docker"
        implementationClass = "dev.monosoul.jooq.JooqDockerPlugin"
        version = project.version
    }
}

pluginBundle {
    website = "https://github.com/monosoul/jooq-gradle-plugin"
    vcsUrl = "https://github.com/monosoul/jooq-gradle-plugin"

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
}

afterEvaluate {
    tasks.jacocoTestReport {
        classDirectories.setFrom(classDirectories.files.map {
            fileTree(it) {
                exclude("dev/monosoul/shaded/org/testcontainers/**/*")
            }
        })
    }
}


dependencies {
    implementation("org.jooq:jooq-codegen:3.16.6")
    implementation("org.glassfish.jaxb:jaxb-runtime:3.0.2")
    implementation("com.github.docker-java:docker-java-transport-okhttp:3.2.13")
    implementation("org.flywaydb:flyway-core:8.5.11")
    implementation("org.flywaydb:flyway-mysql:8.5.11")
    implementation("org.flywaydb:flyway-sqlserver:8.5.11")
    implementation("org.zeroturnaround:zt-exec:1.12")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")

    testImplementation(enforcedPlatform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.strikt:strikt-jvm:0.34.1")
    testImplementation("org.spockframework:spock-core:2.1-groovy-3.0") {
        exclude("org.codehaus.groovy")
    }
    testCompileOnly(gradleTestKit())
}
