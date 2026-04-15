pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SecureChat"

include(":app")
include(":stealthx-crypto")
include(":stealthx-ifr")
include(":security")
include(":shared")
include(":data")
include(":domain")
include(":features:messenger")
include(":features:contacts")
include(":features:settings")
include(":presentation")
include(":transport")
