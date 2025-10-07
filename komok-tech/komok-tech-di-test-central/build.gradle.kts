import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
    mavenLocal()
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
    implementation(libs.logback)
    ksp(libs.komok.tech.di)
    implementation(libs.komok.tech.di.lib)
}
