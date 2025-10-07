import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    implementation(projects.komokTech.komokTechDiLib)
    ksp(projects.komokTech.komokTechDi)
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
