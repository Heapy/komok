import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

plugins {
    jacoco
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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
            freeCompilerArgs.addAll(
                "-Xcontext-parameters",
            )
        }
    }

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "2048m"
}

application {
    mainClass.set("io.heapy.komok.Application")
    applicationName = "backend"
}

tasks.distTar {
    archiveFileName = "komok-app.tar"
}

tasks.distZip {
    enabled = false
}

dependencies {
    implementation(projects.komokAuthCommon)
    implementation(projects.komokTechConfig)
    implementation(projects.komokTechConfigDotenv)
    implementation(projects.komokTechLogging)
    implementation(projects.komokTechTime)
    implementation(projects.komokDaoMg)
    implementation(projects.komokServerCommon)

    implementation(libs.bouncycastle.bcpkix)

    ksp(projects.komokTechDi)
    implementation(projects.komokTechDiLib)

    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.netty)
    implementation(libs.logback)

    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.caching.headers)
    implementation(libs.ktor.server.status.pages)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
    testImplementation(testFixtures(projects.komokTechTime))
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
