import com.replaymod.gradle.preprocess.PreprocessExtension
import com.replaymod.gradle.preprocess.PreprocessTask
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    java
}

val modPlatform = Platform.of(project)

// Minecraft 26.1 and newer ship unobfuscated, so Yarn (and remapping in general) is gone.
// Those versions need the non-remapping Loom plugin, older ones the remapping one.
// Loom has to be applied before the preprocessor, which inspects the `loom` extension to decide
// whether this project is obfuscated.
apply(plugin = if (modPlatform.isObfuscated) "net.fabricmc.fabric-loom-remap" else "net.fabricmc.fabric-loom")
apply(plugin = libs.plugins.preprocessor.get().pluginId)

val mod_version: String by project
val maven_group: String by project

val mod_name = "Fish's Integrated Minecraft"

version = mod_version
group = maven_group

// Minecraft 26.1+ requires Java 25, everything before it runs on 21.
val targetJavaVersion = if (modPlatform.isObfuscated) 21 else 25

val loaderVersion = when (modPlatform.mcVersion) {
    in 12104..12108 -> "0.16.14"
    260200 -> "0.19.3"
    else -> error("No loader version defined for ${modPlatform.mcVersion}")
}

configure<PreprocessExtension> {
    vars.put("MC", modPlatform.mcVersion)
    vars.put("FABRIC", if (modPlatform.isFabric) 1 else 0)
    vars.put("FORGE", if (modPlatform.isForge) 1 else 0)
    vars.put("NEOFORGE", if (modPlatform.isNeoForge) 1 else 0)
    vars.put("FORGELIKE", if (modPlatform.isForgeLike) 1 else 0)

    keywords.set(mapOf(
        ".java" to PreprocessTask.DEFAULT_KEYWORDS
    ))
}

configure<LoomGradleExtensionAPI> {
    runs {
        named("client") {
            ideConfigGenerated(true)
            runDir("../../run") //This makes every version run in the same place
        }
    }

    splitEnvironmentSourceSets()
    mods {
        create("fim") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))

    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    //withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(targetJavaVersion)
}

configure<FabricApiExtension> {
    configureDataGeneration {
        client = true
    }
}

repositories {
    maven("https://maven.neoforged.net/releases")
}

dependencies {
    "minecraft"("com.mojang:minecraft:${modPlatform.mcVersionStr}")

    val fabricAPI = when (modPlatform.mcVersion) {
        12104 -> "0.119.2+1.21.4"
        12105 -> "0.128.0+1.21.5"
        12106 -> "0.128.0+1.21.6"
        12107 -> "0.128.2+1.21.7"
        12108 -> "0.130.0+1.21.8"
        260200 -> "0.156.0+26.2"
        else -> error("No API version defined for ${modPlatform.mcVersion}")
    }

    if (modPlatform.isObfuscated) {
        val yarnVersion = when (modPlatform.mcVersion) {
            12104 -> "1.21.4+build.8"
            12105 -> "1.21.5+build.1"
            12106 -> "1.21.6+build.1"
            12107 -> "1.21.7+build.1"
            12108 -> "1.21.8+build.1"
            else -> error("No mappings defined for ${modPlatform.mcVersion}")
        }

        "mappings"("net.fabricmc:yarn:${yarnVersion}:v2")

        // Mod dependencies have to be remapped from intermediary to the mappings used here.
        "modImplementation"("net.fabricmc:fabric-loader:${loaderVersion}")
        "modImplementation"("net.fabricmc.fabric-api:fabric-api:${fabricAPI}")
    } else {
        // Nothing to remap on unobfuscated versions, so these are plain dependencies.
        implementation("net.fabricmc:fabric-loader:${loaderVersion}")
        implementation("net.fabricmc.fabric-api:fabric-api:${fabricAPI}")
    }

    // 1.18.32 blows up on Java 25 (ExceptionInInitializerError in lombok.permit.Permit)
    val lombok = "org.projectlombok:lombok:1.18.46"
    compileOnly(lombok)
    annotationProcessor(lombok)
    testCompileOnly(lombok)
    testAnnotationProcessor(lombok)
}

// Function to get the range of mc versions supported by a version we are building for.
// First value is start of range, second value is end of range or null to leave the range open
val supportedVersionRange: Pair<String, String?> = when (modPlatform.mcVersion) {
    12104 -> "1.21.4" to "1.21.4"
    12105 -> "1.21.5" to "1.21.5"
    12106 -> "1.21.6" to "1.21.8"
    260200 -> "26.2" to "26.2"
    else -> error("Undefined version range for ${modPlatform.mcVersion}")
}

val prettyVersionRange: String =
    if (supportedVersionRange.first == supportedVersionRange.second) supportedVersionRange.first
    else "${supportedVersionRange.first}${supportedVersionRange.second?.let { "-$it" } ?: "+"}"

val fabricMcVersionRange: String =
    ">=${supportedVersionRange.first}${supportedVersionRange.second?.let { " <=$it" } ?: ""}"

val forgeMcVersionRange: String =
    "[${supportedVersionRange.first},${supportedVersionRange.second?.let { "$it]" } ?: ")"}"

val archives_base_name: String by project
base.archivesName = "$archives_base_name ($prettyVersionRange)"

// Remapping Loom produces the shippable jar in `remapJar`; without remapping the plain `jar` task is it.
val outputJarTaskName = if (modPlatform.isObfuscated) "remapJar" else "jar"

tasks {
    processResources {
        val properties = mapOf(
            "version" to mod_version,
            "minecraft_version" to fabricMcVersionRange,
            "loader_version" to loaderVersion
        )

        filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml")) {
            expand(properties)
        }

        inputs.properties(properties)
        exclude("META-INF/mods.toml", "pack.mcmeta")
    }
    named(outputJarTaskName) {
        finalizedBy("copyJar")
    }
    register<Copy>("copyJar") {
        from(named<AbstractArchiveTask>(outputJarTaskName).flatMap { it.archiveFile })
        into("${project.rootDir}/jars")
    }
    clean { delete("${project.rootDir}/jars") }
}

data class Platform(
    val mcMajor: Int,
    val mcMinor: Int,
    val mcPatch: Int,
    val loader: Loader
) {
    val mcVersion = mcMajor * 10000 + mcMinor * 100 + mcPatch
    val mcVersionStr = listOf(mcMajor, mcMinor, mcPatch).dropLastWhile { it == 0 }.joinToString(".")
    val loaderStr = loader.toString().lowercase()

    val isFabric = loader == Loader.Fabric
    val isForge = loader == Loader.Forge
    val isNeoForge = loader == Loader.NeoForge
    val isForgeLike = loader == Loader.Forge || loader == Loader.NeoForge
    val isLegacy = mcVersion <= 11202

    // Minecraft dropped obfuscation with 26.1; from there on there are no mappings to remap against.
    val isObfuscated = mcMajor < 26

    override fun toString(): String {
        return "$mcVersionStr-$loaderStr"
    }

    enum class Loader {
        Fabric,
        Forge,
        NeoForge
    }

    companion object {
        fun of(project: Project): Platform {
            val (versionStr, loaderStr) = project.name.split("-", limit = 2)
            val (major, minor, patch) = versionStr.split('.').map { it.toInt() } + listOf(0)
            val loader = Loader.values().first { it.name.lowercase() == loaderStr.lowercase() }
            return Platform(major, minor, patch, loader)
        }
    }
}
