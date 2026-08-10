pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net")
        maven("https://jitpack.io") {
            content {
                includeGroupByRegex("com\\.github\\..*")
            }
        }
    }

    resolutionStrategy.eachPlugin {
        when (requested.id.id) {
            "com.replaymod.preprocess" -> useModule("com.github.replaymod:preprocessor:${requested.version}")
            "com.replaymod.preprocess-root" -> useModule("com.github.replaymod:preprocessor:${requested.version}")
        }
    }
}

plugins {
    // Auto-provisions the JDKs the toolchains ask for (21 for the obfuscated versions, 25 for 26.2)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    versionCatalogs.create("libs")
}

rootProject.buildFileName = "root.gradle.kts"

listOf(
    "1.21.4-fabric",
    "1.21.5-fabric",
    "1.21.6-fabric",
    "26.2-fabric"
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}