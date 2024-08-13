plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    `komok-publish-conventions`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.komokTechDiLib)
    ksp(projects.komokTechDi)
}
