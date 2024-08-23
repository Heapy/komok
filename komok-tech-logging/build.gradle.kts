plugins {
    alias(libs.plugins.kotlin.jvm)
    `komok-publish-conventions`
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.slf4j.api)
}
