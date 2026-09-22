plugins {
    // Auto-provisions the JDK 25 toolchain (Paper 26.1.2 minimum) when it is not installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "avian-factions"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    }
}

include("avian-api", "avian-core", "avian-factions", "avian-plugin", "avian-testing")
