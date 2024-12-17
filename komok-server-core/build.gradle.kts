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
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            freeCompilerArgs.addAll(
                "-Xsuppress-warning=CONTEXT_RECEIVERS_DEPRECATED",
                "-Xcontext-receivers",
            )
        }
    }

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation("org.http4k:http4k-server-netty:5.41.0.0")
    implementation(projects.komokTechConfig)
    implementation(projects.komokTechDotenv)
    implementation(projects.komokTechLogging)
    ksp(projects.komokTechDi)
    implementation(projects.komokTechDiLib)

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
