plugins {
    application
    kotlin("jvm").version("1.7.20-RC")
    kotlin("plugin.serialization").version("1.7.20-RC")
    id("me.champeau.jmh").version("0.6.7")
}

repositories {
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        languageVersion = "1.7"
        apiVersion = "1.7"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xcontext-receivers",
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("io.heapy.vipassana.Application")
    applicationName = "backend"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")

    implementation("io.netty:netty5-all:5.0.0.Alpha2")

    implementation("com.google.flogger:flogger-system-backend:0.7.4")
    implementation("com.google.flogger:flogger:0.7.4")
//    implementation("com.google.flogger:google-extensions:0.7.4")
//    implementation("com.google.flogger:flogger-slf4j-backend:0.7.4")
//    implementation("com.google.flogger:flogger-log4j-backend:0.7.4")

    implementation("org.apache.logging.log4j:log4j-core:2.19.0")

    implementation("org.slf4j:slf4j-api:2.0.1")
    implementation("ch.qos.logback:logback-classic:1.4.1")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("io.mockk:mockk:1.12.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
}
