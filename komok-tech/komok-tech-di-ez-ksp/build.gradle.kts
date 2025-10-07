import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.ksp)
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
