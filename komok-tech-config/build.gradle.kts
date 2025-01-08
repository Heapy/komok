plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    `komok-publish-conventions`
}

repositories {
    mavenCentral()
}

dependencies {
    api(projects.komokTechConfigDotenv)
    api(projects.komokTechConfigCommon)
    implementation(projects.komokTechDiLib)
    ksp(projects.komokTechDi)

    implementation(libs.kotlinx.serialization.hocon)
}
