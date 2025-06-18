plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.binary.compatibility.validator)
    `komok-publish-conventions`
}

repositories {
    mavenCentral()
}

dependencies {
    api(projects.komokTech.komokTechConfigCommon)
    implementation(projects.komokTech.komokTechDiLib)
    ksp(projects.komokTech.komokTechDi)
}
