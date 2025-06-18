plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.binary.compatibility.validator)
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.slf4j.api)
}
