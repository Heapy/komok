plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback)
    ksp(projects.komokTechDi)
    implementation(projects.komokTechDiLib)

    implementation(libs.junit.jupiter)
    runtimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
