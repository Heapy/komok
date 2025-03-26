import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
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

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(projects.komokTechConfig)
    ksp(projects.komokTechDi)
    implementation(projects.komokTechDiLib)

    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.serialization.json)

    api(libs.ktor.server.cio)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
    runtimeOnly(libs.junit.platform.launcher)
}
