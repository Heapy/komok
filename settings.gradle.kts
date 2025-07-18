enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "komok"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("ktorLibs") {
            from("io.ktor:ktor-version-catalog:3.2.2")
        }
    }
}

include("komok-app")
include("komok-auth-common")
include("komok-dao-mg")
include("komok-server-common")
include("komok-server-cookie-auth")
include("komok-server-core")
include("komok-tech:komok-tech-config")
include("komok-tech:komok-tech-config-common")
include("komok-tech:komok-tech-config-dotenv")
include("komok-tech:komok-tech-core")
include("komok-tech:komok-tech-decorators-ksp")
include("komok-tech:komok-tech-decorators-lib")
include("komok-tech:komok-tech-di")
include("komok-tech:komok-tech-di-ez")
include("komok-tech:komok-tech-di-ez-ksp")
include("komok-tech:komok-tech-di-ez-lib")
include("komok-tech:komok-tech-di-ez-test")
include("komok-tech:komok-tech-di-lib")
include("komok-tech:komok-tech-di-test")
include("komok-tech:komok-tech-di-test-central")
include("komok-tech:komok-tech-di-test-next")
include("komok-tech:komok-tech-ktor-htmx")
include("komok-tech:komok-tech-logging")
include("komok-tech:komok-tech-time")
include("komok-tech:komok-tech-to-be-injected")
