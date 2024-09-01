plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.binary.compatibility.validator)
    `komok-publish-conventions`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.komok.komokTechLogging)

    testImplementation(libs.mockk)
    testImplementation(libs.logback)
    testImplementation(libs.junit.jupiter)
    runtimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
