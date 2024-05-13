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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()

        languageVersion = KotlinVersion.KOTLIN_2_0.version
        apiVersion = KotlinVersion.KOTLIN_2_0.version

        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xcontext-receivers",
        )
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
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.0")
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
