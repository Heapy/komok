plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
}

dependencies {
    implementation(libs.kotlin.poet.ksp.snapshot)
    implementation(libs.ksp)
    implementation(projects.komokTechDecoratorsLib)
}
