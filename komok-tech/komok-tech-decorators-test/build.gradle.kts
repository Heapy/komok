import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(projects.komokTech.komokTechDecoratorsKsp)
    ksp(projects.komokTech.komokTechDecoratorsPluginLogging)
    implementation(projects.komokTech.komokTechDecoratorsPluginLoggingLib)
    implementation(projects.komokTech.komokTechLogging)
    implementation(kotlin("reflect"))
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
