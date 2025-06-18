plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback)
    ksp(projects.komokTech.komokTechDi)
    implementation(projects.komokTech.komokTechDiTest)
    implementation(projects.komokTech.komokTechDiLib)

    implementation(libs.junit.jupiter)
    runtimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
