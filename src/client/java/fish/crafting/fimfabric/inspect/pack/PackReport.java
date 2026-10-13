package fish.crafting.fimfabric.inspect.pack;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * What the active resource packs, vanilla and mods left aside, bring along: how large they are, which
 * fonts they define and which shaders they replace.
 */
public record PackReport(List<PackInfo> packs, List<FontInfo> fonts, List<ShaderInfo> shaders) {

    /**
     * @param source "server", "world" or "local"
     * @param bytesByCategory Uncompressed size per top level asset directory
     */
    public record PackInfo(String id, String title, String source, long bytes, int files, Map<String, Long> bytesByCategory) {
    }

    /**
     * One file of a pack. It is read from the pack on demand, which fails once the packs got reloaded.
     *
     * @param location Path below {@code assets/<namespace>/}
     */
    public record PackFile(String packId, Identifier location, IoSupplier<InputStream> content) {

        public String assetPath(){
            return "assets/" + location.getNamespace() + "/" + location.getPath();
        }

    }

    public record FontInfo(Identifier id, List<FontDefinition> definitions) {
    }

    /**
     * @param error Why the providers could not be read, null if they could
     */
    public record FontDefinition(PackFile file, List<FontProvider> providers, @Nullable String error) {
    }

    /**
     * @param reference What the provider points at: a texture, a font file or another font
     * @param referenced The file behind the reference, if a pack ships it
     * @param codepoints Characters of a bitmap provider, row by row
     * @param advances Characters of a space provider and how far they move the cursor
     */
    public record FontProvider(String type, @Nullable Identifier reference, @Nullable PackFile referenced,
                               int height, int ascent, int[] codepoints, Map<Integer, Float> advances, JsonObject raw) {
    }

    /**
     * @param kind "core", "include", "post", "post_effect" or "other"
     * @param active Whether this is the file the game ends up using, false if another pack sits on top
     * @param vanilla The file this one replaces, null if vanilla has no such file
     */
    public record ShaderInfo(Identifier location, String kind, PackFile file, boolean active, @Nullable PackFile vanilla) {
    }

}
