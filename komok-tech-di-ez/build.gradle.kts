plugins {
    alias(libs.plugins.kotlin.jvm)
//    `komok-publish-conventions`
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.slf4j.api)

    testImplementation(kotlin("reflect"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
    runtimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
