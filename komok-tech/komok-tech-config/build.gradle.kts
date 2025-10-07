import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

kotlin {
    jvmToolchain(25)
}

java {
    targetCompatibility = JavaVersion.VERSION_21
}

tasks
    .withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
    .configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.addAll(
                "-Xcontext-parameters",
            )
        }
    }

dependencies {
    api(projects.komokTech.komokTechConfigDotenv)
    api(projects.komokTech.komokTechConfigCommon)
    implementation(projects.komokTech.komokTechDiLib)
    ksp(projects.komokTech.komokTechDi)

    implementation(libs.kotlinx.serialization.hocon)
}
