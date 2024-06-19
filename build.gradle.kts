import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    jacoco
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

tasks
    .withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
    .configureEach {
        compilerOptions {
            jvmTarget.set(JVM_21)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("io.heapy.komok.Application")
    applicationName = "backend"
}

dependencies {
    implementation(projects.database)

    implementation(kotlin("reflect"))

    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.hocon)

    implementation(libs.hikari)

    implementation(libs.logback)

    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
    implementation("io.ktor:ktor-client-cio:2.3.11")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-server-cio:2.3.11")
    implementation("io.ktor:ktor-server-locations:2.3.11")
    implementation("io.ktor:ktor-server-websockets:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.11")
    implementation("io.ktor:ktor-server-metrics-micrometer:2.3.11")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.1")
    implementation("io.ktor:ktor-server-call-logging:2.3.11")
    implementation("io.ktor:ktor-server-default-headers:2.3.11")
    implementation("io.ktor:ktor-server-caching-headers:2.3.11")
    implementation("io.ktor:ktor-server-status-pages:2.3.11")

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
    runtimeOnly(libs.junit.platform.launcher)
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        csv.required = false
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
}
