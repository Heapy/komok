plugins {
    signing
    `java-library`
    `maven-publish`
}

group = "io.heapy.komok"

java {
    withJavadocJar()
    withSourcesJar()
}

val modules: Map<String, Map<String, String>> = mapOf(
    "komok-tech-di" to mapOf(
        "publishName" to "Library for dependency injection in Kotlin",
        "publishDescription" to "KSP plugin for dependency injection in Kotlin",
    ),
    "komok-tech-di-lib" to mapOf(
        "publishName" to "Komok Tech DI Support Library",
        "publishDescription" to "Support library for komok-tech-di",
    ),
    "komok-tech-di-ez" to mapOf(
        "publishName" to "Komok Tech DI Easy",
        "publishDescription" to "Easy way for dependency injection in Kotlin",
    ),
    "komok-tech-config-dotenv" to mapOf(
        "publishName" to "Komok Tech Dotenv",
        "publishDescription" to "Simple .env implementation",
    ),
    "komok-tech-config" to mapOf(
        "publishName" to "Komok Tech Config",
        "publishDescription" to "Configuration module based on .env and HOCON",
    ),
    "komok-tech-config-common" to mapOf(
        "publishName" to "Komok Tech Config",
        "publishDescription" to "Configuration module based on .env and HOCON",
    ),
    "komok-tech-logging" to mapOf(
        "publishName" to "Komok Tech Logging",
        "publishDescription" to "Logging module based on SLF4J",
    ),
    "komok-tech-to-be-injected" to mapOf(
        "publishName" to "Komok Tech To Be Injected",
        "publishDescription" to "Library for dependency injection in Kotlin",
    ),
    "komok-tech-time" to mapOf(
        "publishName" to "Komok Tech Time",
        "publishDescription" to "Library for working with time",
    ),
    "komok-tech-ktor-htmx" to mapOf(
        "publishName" to "Komok Tech Ktor HTMX",
        "publishDescription" to "HTMX support for Ktor",
    ),
    "komok-tech-decorators-ksp" to mapOf(
        "publishName" to "TODO",
        "publishDescription" to "TODO",
    ),
    "komok-tech-decorators-ksp-api" to mapOf(
        "publishName" to "TODO",
        "publishDescription" to "TODO",
    ),
    "komok-tech-decorators-lib" to mapOf(
        "publishName" to "TODO",
        "publishDescription" to "TODO",
    ),
    "komok-tech-decorators-plugin-logging" to mapOf(
        "publishName" to "TODO",
        "publishDescription" to "TODO",
    ),
    "komok-tech-decorators-plugin-logging-lib" to mapOf(
        "publishName" to "TODO",
        "publishDescription" to "TODO",
    ),
    "komok-tech-stable-values" to mapOf(
        "publishName" to "Komok Tech: JEP 502 Stable Values API for Kotlin",
        "publishDescription" to "Provides property delegate for stable supplier and convenience functions for stable collections and functions.",
    )
)

fun Project.getPublishName(): String = modules.getValue(name).getValue("publishName")
fun Project.getPublishDescription(): String = modules.getValue(name).getValue("publishDescription")

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = project.name

            from(components["java"])

            pom {
                name = project.getPublishName()
                description = project.getPublishDescription()
                url = "https://github.com/Heapy/komok"
                inceptionYear = "2024"
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://spdx.org/licenses/Apache-2.0.html"
                    }
                }
                developers {
                    developer {
                        id = "ruslan.ibrahimau"
                        name = "Ruslan Ibrahimau"
                        email = "ruslan@heapy.io"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/Heapy/komok.git"
                    developerConnection = "scm:git:ssh://github.com/Heapy/komok.git"
                    url = "https://github.com/Heapy/komok"
                }
            }
        }
    }

    repositories {
        maven {
            url = rootProject.layout.buildDirectory
                .dir("staging-deploy")
                .get().asFile.toURI()
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
