plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.ksp)
    `komok-publish-conventions`
}

repositories {
    mavenCentral()
}

dependencies {
    api(projects.komokTech.komokTechConfigDotenv)
    api(projects.komokTech.komokTechConfigCommon)
    implementation(projects.komokTech.komokTechDiLib)
    ksp(projects.komokTech.komokTechDi)

    implementation(libs.kotlinx.serialization.hocon)
}
