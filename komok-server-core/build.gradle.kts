import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation("org.http4k:http4k-server-netty:6.23.1.0")
    implementation("io.undertow:undertow-core:2.3.20.Final")
    implementation(projects.komokTech.komokTechConfig)
    implementation(projects.komokTech.komokTechConfigDotenv)
    implementation(projects.komokTech.komokTechLogging)
    ksp(projects.komokTech.komokTechDi)
    implementation(projects.komokTech.komokTechDiLib)

    implementation(libs.logback)

    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.serialization.json)

    api(libs.netty)
    implementation(libs.netty.kqueue)
    implementation(libs.netty.epoll)
    implementation(libs.netty.iouring)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
    runtimeOnly(libs.junit.platform.launcher)
}
