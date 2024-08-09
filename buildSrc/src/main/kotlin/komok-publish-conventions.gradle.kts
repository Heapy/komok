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
