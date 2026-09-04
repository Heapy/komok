@file:OptIn(kotlinx.validation.ExperimentalBCVApi::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.binary.compatibility.validator)
    `komok-publish-conventions`
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }

        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }

    js {
        nodejs()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    iosArm64()
    iosSimulatorArm64()
    linuxArm64()
    linuxX64()
    macosArm64()
    mingwX64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    jvmToolchain(25)

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xname-based-destructuring=complete",
            "-Xreturn-value-checker=full",
        )
        allWarningsAsErrors = true
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(libs.slf4j.api)
        }

        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
            implementation(libs.mockk)
            implementation(libs.logback)
            implementation(libs.junit.jupiter)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}

apiValidation {
    klib {
        enabled = true
    }
}
