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
    implementation(projects.komokTech.komokTechConfig)
    implementation(projects.komokTech.komokTechConfigDotenv)
    implementation(projects.komokTech.komokTechLogging)
    implementation(projects.komokTech.komokTechTime)
    implementation(projects.komokDaoMg)
    implementation(projects.komokServerCommon)

    implementation(libs.bouncycastle.bcpkix)

    ksp(projects.komokTech.komokTechDi)
    implementation(projects.komokTech.komokTechDiLib)

    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.netty)
    implementation(libs.logback)

    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.client.cio)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.cio)
    implementation(ktorLibs.server.resources)
    implementation(ktorLibs.server.websockets)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.cachingHeaders)
    implementation(ktorLibs.server.statusPages)

    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
    testImplementation(testFixtures(projects.komokTech.komokTechTime))
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
