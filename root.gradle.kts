plugins {
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.loomRemap) apply false
    alias(libs.plugins.preprocessorRoot)
}

preprocess {
    strictExtraMappings.set(true)

    val fabric12104 = createNode("1.21.4-fabric", 12104, "yarn")
    val fabric12105 = createNode("1.21.5-fabric", 12105, "yarn")
    val fabric12106 = createNode("1.21.6-fabric", 12106, "yarn")
    // Minecraft 26.1+ ships unobfuscated, so there are no mappings at all (Yarn ended with 1.21.11).
    // `null` tells the preprocessor to bridge Yarn -> Mojang names on its own.
    val fabric2602 = createNode("26.2-fabric", 260200, null)

    fabric12106.link(fabric12105)
    fabric12105.link(fabric12104)
    fabric12106.link(fabric2602, file("versions/mapping-1.21.6-26.2.txt"))
}