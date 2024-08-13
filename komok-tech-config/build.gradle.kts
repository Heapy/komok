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
    implementation(projects.komokTechDotenv)
    implementation(projects.komokTechDiLib)
    ksp(projects.komokTechDi)

    implementation(libs.kotlinx.serialization.hocon)

}
