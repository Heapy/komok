plugins {
    alias(libs.plugins.kotlin.jvm)
    `komok-publish-conventions`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.poet.ksp)
    implementation(libs.ksp)
}
