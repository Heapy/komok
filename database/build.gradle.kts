plugins {
    application
    alias(libs.plugins.kotlin.jvm)
}

application {
    mainClass.set("io.heapy.komok.migration.MigrationKt")
    applicationName = "migrations"
}

tasks.distTar {
    enabled = false
}

tasks.distZip {
    enabled = false
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.jooq.core)
    api(libs.postgresql)
}
