import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm").version("2.0.20-Beta2")
    id("com.google.devtools.ksp").version("2.0.20-Beta2-1.0.23")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvmToolchain(21)
}

tasks
    .withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
    .configureEach {
        compilerOptions {
            jvmTarget.set(JVM_21)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

dependencies {
    implementation(libs.logback)
    ksp("io.heapy.komok:komok-tech-di:1.0.1")
    implementation("io.heapy.komok:komok-tech-di-lib:1.0.1")
}
