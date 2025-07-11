import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

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
    implementation(libs.logback)
    implementation(projects.komokTech.komokTechDecoratorsKspApi)
}

tasks
    .withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
    .configureEach {
        compilerOptions {
            jvmTarget.set(JVM_21)
            freeCompilerArgs.addAll(
                "-Xcontext-parameters",
            )
        }
    }
