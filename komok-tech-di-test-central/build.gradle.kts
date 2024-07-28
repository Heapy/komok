plugins {
    kotlin("jvm").version("2.0.20-Beta2")
    id("com.google.devtools.ksp").version("2.0.20-Beta2-1.0.23")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(libs.logback)
    ksp("io.heapy.komok:komok-tech-di:1.0.0")
    implementation("io.heapy.komok:komok-tech-di-lib:1.0.0")
}
