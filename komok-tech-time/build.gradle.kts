plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.ksp)
    `komok-publish-conventions`
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.komokTechDiLib)
    ksp(projects.komokTechDi)
}
