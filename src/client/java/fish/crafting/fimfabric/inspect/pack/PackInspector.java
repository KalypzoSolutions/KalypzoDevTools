package fish.crafting.fimfabric.inspect.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fish.crafting.fimfabric.client.FIMModClient;
import fish.crafting.fimfabric.inspect.pack.PackReport.*;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Reads everything the active packs are opened for anyway, so it is slow on large packs and belongs
 * on a background thread.
 */
public class PackInspector {

    //A zip pack can only be listed below a directory, so these are spelled out. The game reads no others.
    private static final List<String> ASSET_DIRECTORIES = List.of(
            "atlases", "blockstates", "equipment", "font", "items", "lang", "models", "particles",
            "post_effect", "shaders", "sounds", "texts", "textures", "waypoint_style"
    );
    private static final FileToIdConverter FONT_DEFINITIONS = FileToIdConverter.json("font");

    public static PackReport inspect(ResourceManager manager){
        List<PackInfo> packs = manager.listPacks()
                .filter(PackInspector::isForeign)
                .map(PackInspector::measure)
                .toList();

        return new PackReport(packs, fonts(manager), shaders(manager));
    }

    /**
     * Vanilla is built in and mods use a source of their own, which leaves the packs somebody picked.
     */
    private static boolean isForeign(PackResources pack){
        return sourceName(pack) != null;
    }

    private static @Nullable String sourceName(PackResources pack){
        PackSource source = pack.location().source();
        if(source == PackSource.SERVER) return "server";
        if(source == PackSource.WORLD) return "world";
        if(source == PackSource.DEFAULT) return "local";
        return null;
    }

    private static PackInfo measure(PackResources pack){
        Map<String, Long> bytesByCategory = new TreeMap<>();
        long[] total = new long[2];

        for (String namespace : pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
            for (String directory : ASSET_DIRECTORIES) {
                pack.listResources(PackType.CLIENT_RESOURCES, namespace, directory, (id, content) -> {
                    long size = sizeOf(content);
                    bytesByCategory.merge(directory, size, Long::sum);
                    total[0] += size;
                    total[1]++;
                });
            }
        }

        return new PackInfo(pack.packId(), pack.location().title().getString(), sourceName(pack),
                total[0], (int) total[1], bytesByCategory);
    }

    private static long sizeOf(IoSupplier<InputStream> content){
        try(InputStream in = content.get()){
            return in.transferTo(OutputStream.nullOutputStream());
        }catch (IOException e){
            return 0;
        }
    }

    private static List<FontInfo> fonts(ResourceManager manager){
        List<FontInfo> fonts = new ArrayList<>();

        FONT_DEFINITIONS.listMatchingResourceStacks(manager).forEach((file, stack) -> {
            List<FontDefinition> definitions = new ArrayList<>();
            for (Resource resource : stack) {
                if(!isForeign(resource.source())) continue;
                definitions.add(readDefinition(manager, new PackFile(resource.sourcePackId(), file, resource::open)));
            }

            if(!definitions.isEmpty()) fonts.add(new FontInfo(FONT_DEFINITIONS.fileToId(file), definitions));
        });

        fonts.sort(Comparator.comparing(font -> font.id().toString()));
        return fonts;
    }

    private static FontDefinition readDefinition(ResourceManager manager, PackFile file){
        List<FontProvider> providers = new ArrayList<>();

        try(Reader reader = new InputStreamReader(file.content().get(), StandardCharsets.UTF_8)){
            JsonArray array = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("providers");
            if(array != null){
                for (JsonElement element : array) {
                    providers.add(readProvider(manager, element.getAsJsonObject()));
                }
            }
        }catch (Exception e){
            //Server packs are regularly broken on purpose, the file itself is still worth listing
            FIMModClient.LOGGER.warn("Could not read font definition {} of {}", file.location(), file.packId(), e);
            return new FontDefinition(file, providers, e.toString());
        }

        return new FontDefinition(file, providers, null);
    }

    private static FontProvider readProvider(ResourceManager manager, JsonObject json){
        String type = json.has("type") ? json.get("type").getAsString() : "unknown";
        //Vanilla accepts the type with or without its namespace
        if(type.startsWith("minecraft:")) type = type.substring("minecraft:".length());

        Identifier reference = null;
        Identifier location = null;
        int[] codepoints = new int[0];
        Map<Integer, Float> advances = new LinkedHashMap<>();

        switch (type) {
            case "bitmap" -> {
                reference = id(json, "file");
                if(reference != null) location = reference.withPrefix("textures/");
                if(json.has("chars")){
                    StringBuilder chars = new StringBuilder();
                    json.getAsJsonArray("chars").forEach(row -> chars.append(row.getAsString()));
                    codepoints = chars.codePoints().toArray();
                }
            }
            case "ttf" -> {
                reference = id(json, "file");
                if(reference != null) location = reference.withPrefix("font/");
            }
            case "unihex" -> {
                reference = id(json, "hex_file");
                location = reference;
            }
            case "reference" -> reference = id(json, "id");
            case "space" -> {
                if(json.has("advances")){
                    json.getAsJsonObject("advances").entrySet().forEach(entry ->
                            advances.put(entry.getKey().codePointAt(0), entry.getValue().getAsFloat()));
                }
            }
            default -> {
            }
        }

        //The same defaults vanilla falls back to
        int height = json.has("height") ? json.get("height").getAsInt() : 8;
        int ascent = json.has("ascent") ? json.get("ascent").getAsInt() : 0;

        return new FontProvider(type, reference, location == null ? null : foreignFile(manager, location),
                height, ascent, codepoints, advances, json);
    }

    private static @Nullable Identifier id(JsonObject json, String key){
        return json.has(key) ? Identifier.tryParse(json.get(key).getAsString()) : null;
    }

    private static @Nullable PackFile foreignFile(ResourceManager manager, Identifier location){
        Resource resource = manager.getResource(location).orElse(null);
        if(resource == null || !isForeign(resource.source())) return null;

        return new PackFile(resource.sourcePackId(), location, resource::open);
    }

    private static List<ShaderInfo> shaders(ResourceManager manager){
        List<ShaderInfo> shaders = new ArrayList<>();

        for (String directory : List.of("shaders", "post_effect")) {
            manager.listResourceStacks(directory, id -> true).forEach((location, stack) -> {
                PackFile vanilla = null;
                for (Resource resource : stack) {
                    if(resource.source().location().source() == PackSource.BUILT_IN){
                        vanilla = new PackFile(resource.sourcePackId(), location, resource::open);
                    }
                }

                for (Resource resource : stack) {
                    if(!isForeign(resource.source())) continue;

                    PackFile file = new PackFile(resource.sourcePackId(), location, resource::open);
                    //The stack is ordered like the packs, the last entry is the one that gets loaded
                    shaders.add(new ShaderInfo(location, kindOf(location), file, resource == stack.getLast(), vanilla));
                }
            });
        }

        shaders.sort(Comparator.comparing(shader -> shader.location().toString()));
        return shaders;
    }

    private static String kindOf(Identifier location){
        String path = location.getPath();
        if(path.startsWith("shaders/core/")) return "core";
        if(path.startsWith("shaders/include/")) return "include";
        if(path.startsWith("shaders/post/")) return "post";
        if(path.startsWith("post_effect/")) return "post_effect";
        return "other";
    }

}
