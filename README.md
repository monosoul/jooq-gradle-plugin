# Gradle Docker jOOQ Plugin

[![Build Status](https://github.com/monosoul/jooq-gradle-plugin/actions/workflows/build-on-push.yml/badge.svg?branch=master)](https://github.com/monosoul/jooq-gradle-plugin/actions/workflows/build-on-push.yml?query=master)
[![codecov](https://codecov.io/gh/monosoul/jooq-gradle-plugin/branch/master/graph/badge.svg?token=7SWSOTIBMX)](https://codecov.io/gh/monosoul/jooq-gradle-plugin)
[![GitHub Release](https://img.shields.io/github/release/monosoul/jooq-gradle-plugin.svg?label=GitHub%20Release)](https://github.com/monosoul/jooq-gradle-plugin/releases)
[![Gradle Plugins Release](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/dev/monosoul/jooq-docker/dev.monosoul.jooq-docker.gradle.plugin/maven-metadata.xml.svg?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/dev.monosoul.jooq-docker)
[![license](https://img.shields.io/github/license/monosoul/jooq-gradle-plugin.svg)](LICENSE)

Copyright 2021 [Adrian Skrobacz](https://github.com/adrianskrobaczrevolut)

Copyright 2021 Revolut Ltd

Copyright 2022 [Andrei Nevedomskii](https://github.com/monosoul)

---

Notice: this plugin was originally developed [here](https://github.com/revolut-engineering/jooq-plugin), but since
I can't publish it under the same group, I had to change the group from `com.revolut` to `dev.monosoul`.

---

This repository contains Gradle plugin for generating jOOQ classes in dockerized databases.
Plugin registers task `generateJooqClasses` that does following steps:

* pulls docker image
* starts database container
* runs migrations using Flyway
* generates jOOQ classes

**Use**:

- **`0.3.x` and later for jOOQ versions `3.12.x` and later**
- **`0.2.x` and later releases for jOOQ versions `3.11.x` and later**
- **For earlier versions use `0.1.x` release**

# Examples

By default plugin is configured to work with PostgreSQL, so the following minimal config is enough:

```kotlin
plugins {
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jooq:jooq:3.16.5")
    jooqCodegen("org.postgresql:postgresql:42.3.6")
}
```

It will look for migration files in `src/main/resources/db/migration` and will output generated classes
to `build/generated-jooq` in package `org.jooq.generated`. All of that details can be configured on the task itself
as shown in examples below.

Configuring schema names and other parameters of the task:

```kotlin
import dev.monosoul.jooq.RecommendedVersions

plugins {
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

tasks {
    generateJooqClasses {
        schemas.set(listOf("public", "other_schema"))
        basePackageName.set("org.jooq.generated")
        inputDirectory.setFrom(project.files("src/main/resources/db/migration"))
        outputDirectory.set(project.layout.buildDirectory.dir("generated-jooq"))
        flywayProperties.put("flyway.placeholderReplacement", "false")
        includeFlywayTable.set(true)
        outputSchemaToDefault.add("public")
        schemaToPackageMapping.put("public", "fancy_name")
        usingJavaConfig {
            /* "this" here is the org.jooq.meta.jaxb.Generator configure it as you please */
        }
    }
}

dependencies {
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
    jooqCodegen("org.postgresql:postgresql:42.3.6")
}
```

To configure the plugin to use another version or edition of Flyway the following config can be used:

```kotlin
import dev.monosoul.jooq.RecommendedVersions

plugins {
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
    jooqCodegen("org.postgresql:postgresql:42.3.6")
    jooqCodegen("org.flywaydb.enterprise:flyway-core:${RecommendedVersions.FLYWAY_VERSION}")
}
```

To configure the plugin to use another version or edition of jOOQ the following config can be used:

```kotlin
plugins {
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

val jooqVersion = "3.15.10"

dependencies {
    implementation("org.jooq:jooq:$jooqVersion")
    jooqCodegen("org.postgresql:postgresql:42.3.6")
    jooqCodegen("org.jooq:jooq-codegen:$jooqVersion")
}
```

To configure the plugin to work with another DB like MySQL the following config can be applied:

```kotlin
import dev.monosoul.jooq.RecommendedVersions

plugins {
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

jooq {
    withContainer {
        image {
            name = "mysql:8.0.29"
            envVars = mapOf(
                "MYSQL_ROOT_PASSWORD" to "mysql",
                "MYSQL_DATABASE" to "mysql"
            )
        }

        db {
            username = "root"
            password = "mysql"
            name = "mysql"
            port = 3306

            jdbc {
                schema = "jdbc:mysql"
                driverClassName = "com.mysql.cj.jdbc.Driver"
            }
        }
    }
}

dependencies {
    implementation("org.jooq:jooq:3.16.5")
    jooqCodegen("mysql:mysql-connector-java:8.0.29")
    jooqCodegen("org.flywaydb:flyway-mysql:${RecommendedVersions.FLYWAY_VERSION}")
}
```

To register custom types:

```kotlin
plugins {
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

tasks {
    generateJooqClasses {
        usingJavaConfig {
            database.withForcedTypes(
                ForcedType()
                    .withUserType("com.google.gson.JsonElement")
                    .withBinding("com.example.PostgresJSONGsonBinding")
                    .withTypes("JSONB")
            )
        }
    }
}

dependencies {
    implementation("org.jooq:jooq:3.16.5")
    jooqCodegen("org.postgresql:postgresql:42.3.6")
}
```

To use XML-based configuration:

```kotlin
plugins {
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

tasks {
    generateJooqClasses {
        usingXmlConfig()
    }
}

dependencies {
    implementation("org.jooq:jooq:3.16.5")
    jooqCodegen("org.postgresql:postgresql:42.3.6")
}
```

where `src/main/resources/db/jooq.xml` looks as following:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<configuration xmlns="http://www.jooq.org/xsd/jooq-codegen-3.16.5.xsd">
    <generator>
        <database>
            <inputSchema>public</inputSchema>
            <includes>.*</includes>
            <forcedTypes>
                <forcedType>
                    <includeTypes>JSONB</includeTypes>
                    <userType>com.google.gson.JsonElement</userType>
                    <binding>com.example.PostgresJSONGsonBinding</binding>
                </forcedType>
            </forcedTypes>
        </database>
    </generator>
</configuration>
```

To exclude flyway schema history table from generated classes:

```kotlin
plugins {
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

tasks {
    generateJooqClasses {
        schemas.set(listOf("other"))
        usingJavaConfig {
            database.withExcludes("flyway_schema_history")
        }
    }
}

dependencies {
    implementation("org.jooq:jooq:3.16.5")
    jooqCodegen("org.postgresql:postgresql:42.3.6")
}
```

### Remote docker setup

The plugin uses [testcontainers library](https://www.testcontainers.org) to spin up the DB
container. If you want to use the plugin with remote docker instance, refer to the
[testcontainers documentation](https://www.testcontainers.org/features/configuration/#customizing-docker-host-detection)
.

### Remote database setup

The plugin supports remote database setup, where an external DB can be used to generate jOOQ classes instead of
spinning up a container with the DB. This setup can also be convenient when a container with the DB is created
externally (for example with Docker compose).

To use the plugin with a remote DB:

```kotlin
plugins {
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

jooq {
    withoutContainer {
        db {
            username = "postgres"
            password = "postgres"
            name = "postgres"
            host = "remotehost"
            port = 5432
        }
    }
}

dependencies {
    jooqCodegen("org.postgresql:postgresql:42.3.6")
}
```

### Multi-database setup

The plugin supports multi-database setup, where jOOQ classes could be generated out of different RDBMS.
This could be achieved by registering a separate class generation task for every RDBMS.

Here's an example how to generate jOOQ classes for PostgreSQL and MySQL in a single project:

```kotlin
import dev.monosoul.jooq.GenerateJooqClassesTask
import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.6.21"
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

tasks {
    generateJooqClasses {
        basePackageName.set("org.jooq.generated.postgres")
        inputDirectory.setFrom("src/main/resources/postgres/migration")
        outputDirectory.set(project.layout.buildDirectory.dir("postgres"))
    }

    register<GenerateJooqClassesTask>("generateJooqClassesForMySql") {
        basePackageName.set("org.jooq.generated.mysql")
        inputDirectory.setFrom("src/main/resources/mysql/migration")
        outputDirectory.set(project.layout.buildDirectory.dir("mysql"))

        withContainer {
            image {
                name = "mysql:8.0.29"
                envVars = mapOf(
                    "MYSQL_ROOT_PASSWORD" to "mysql",
                    "MYSQL_DATABASE" to "mysql"
                )
            }
            db {
                username = "root"
                password = "mysql"
                name = "mysql"
                port = 3306

                jdbc {
                    schema = "jdbc:mysql"
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                }
            }
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    jooqCodegen("org.postgresql:postgresql:42.3.6")
    jooqCodegen("mysql:mysql-connector-java:8.0.29")
    jooqCodegen("org.flywaydb:flyway-mysql:${RecommendedVersions.FLYWAY_VERSION}")
    implementation("org.jooq:jooq:3.16.6")
}
```

where:

- For PostgreSQL:
    - migrations are located in `src/main/resources/postgres/migration`
    - generated classes are located in `build/postgres` under `org.jooq.generated.postgres` package
- For MySQL:
    - migrations are located in `src/main/resources/mysql/migration`
    - generated classes are located in `build/mysql` under `org.jooq.generated.mysql` package

Basically, the plugin has 2 sets of configurations: **global** (or project-wide) configuration declared within `jooq {}`
block and **local** (or task-specific) configuration declared for each task separately.

Local (or task-specific) configuration initial values are inherited from the global (or project-wide) configuration.
So if you modify the global configuration first, and then modify the local configuration, the local configuration's
initial values will be equal to the global configuration's values.

Modifying the local configuration *will not affect* the global configuration.

### Configuration with properties

The plugin supports configuration with properties.

Here's an example of how to use `gradle.properties` file to configure the plugin to generate jOOQ classes for MySQL:

`gradle.properties`:

```properties
dev.monosoul.jooq.withContainer.db.username=root
dev.monosoul.jooq.withContainer.db.password=mysql
dev.monosoul.jooq.withContainer.db.name=mysql
dev.monosoul.jooq.withContainer.db.port=3306
dev.monosoul.jooq.withContainer.db.jdbc.schema=jdbc:mysql
dev.monosoul.jooq.withContainer.db.jdbc.driverClassName=com.mysql.cj.jdbc.Driver
dev.monosoul.jooq.withContainer.image.name=mysql:8.0.29
dev.monosoul.jooq.withContainer.image.envVars.MYSQL_ROOT_PASSWORD=mysql
dev.monosoul.jooq.withContainer.image.envVars.MYSQL_DATABASE=mysql
```

`build.gradle.kts`:

```kotlin
import dev.monosoul.jooq.RecommendedVersions

plugins {
    kotlin("jvm") version "1.6.21"
    id("dev.monosoul.jooq-docker")
}

repositories {
    mavenCentral()
}

dependencies {
    jooqCodegen("mysql:mysql-connector-java:8.0.29")
    jooqCodegen("org.flywaydb:flyway-mysql:${RecommendedVersions.FLYWAY_VERSION}")
    implementation("org.jooq:jooq:${RecommendedVersions.JOOQ_VERSION}")
}
```

And here's an example how to customize the plugin configuration from command line:

```shell
./gradlew build -Pdev.monosoul.jooq.withContainer.db.username=root -Pdev.monosoul.jooq.withContainer.db.password=password
```

#### ❗ NOTE: `withoutContainer` properties have higher priority than `withContainer` properties.

#### ❗ NOTE: properties only affect global (or project-wide) configuration.
