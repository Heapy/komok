plugins {
    alias(libs.plugins.kotlin.jvm)
    `komok-publish-conventions`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.ksp)
}
