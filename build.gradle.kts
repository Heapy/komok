import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    application
    kotlin("jvm").version("1.9.0")
    kotlin("plugin.serialization").version("1.9.0")
}

repositories {
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()

        languageVersion = KotlinVersion.KOTLIN_1_9.version
        apiVersion = KotlinVersion.KOTLIN_1_9.version

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
    api(kotlin("reflect"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.1")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.6.0")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}
