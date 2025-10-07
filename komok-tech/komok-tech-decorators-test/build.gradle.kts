import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

dependencies {
//    ksp(projects.komokTech.komokTechDecoratorsKsp)
//    ksp(projects.komokTech.komokTechDecoratorsPluginLogging)
    implementation(projects.komokTech.komokTechDecoratorsPluginLoggingLib)
    implementation(projects.komokTech.komokTechLogging)
    implementation(kotlin("reflect"))
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
