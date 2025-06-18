plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.binary.compatibility.validator)
    `komok-publish-conventions`
}

repositories {
    mavenCentral()
}

dependencies {
    api(ktorLibs.server.core)
    api(libs.kotlinx.html)

    testImplementation(ktorLibs.server.cio)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
    runtimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
