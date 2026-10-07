pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net") {
            name = "Fabric"
        }
        gradlePluginPortal()
    }
}

plugins {
    // Auto-provisions the JDK the toolchain asks for
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "FIM-Fabric"
