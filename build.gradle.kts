plugins {
    alias(libs.plugins.loom)
}

val modVersion = providers.gradleProperty("mod_version").get()

val minecraftVersion = libs.versions.minecraft.get()
val javaVersion = 25

group = providers.gradleProperty("maven_group").get()
// Only names the jar; fabric.mod.json gets the plain mod_version
version = "$modVersion+mc$minecraftVersion"
base.archivesName = providers.gradleProperty("archives_base_name")

loom {
    splitEnvironmentSourceSets()

    mods {
        register("fim") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }

    runs {
        // Client-only mod, a server run config would just be noise
        named("server") {
            ideConfigGenerated(false)
        }
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = javaVersion
        options.encoding = "UTF-8"
    }

    processResources {
        val properties = mapOf(
            "version" to modVersion,
            "minecraft_version" to minecraftVersion,
            "loader_version" to libs.versions.fabric.loader.get(),
            "java_version" to javaVersion
        )

        inputs.properties(properties)
        filesMatching("fabric.mod.json") {
            expand(properties)
        }
    }
}
