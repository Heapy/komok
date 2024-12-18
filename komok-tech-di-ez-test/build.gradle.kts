plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback)
    ksp(projects.komokTechDiEzKsp)
    implementation(projects.komokTechDiEzLib)

    implementation(libs.junit.jupiter)
    runtimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
