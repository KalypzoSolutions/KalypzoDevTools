package fish.crafting.fimfabric.minimessage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fish.crafting.fimfabric.client.FIMModClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Knows what the images of a CraftEngine server look like on the client.
 *
 * <p>Two sources, the file wins:
 * <ol>
 *   <li>The active resource packs. The server's pack ships its images as bitmap fonts, so every
 *       private use glyph of a bitmap provider becomes an image. The id comes from the texture:
 *       {@code craftengine:font/image/default/bow.png} is {@code default:bow}. Providers with more than
 *       one glyph add {@code :row:column}.</li>
 *   <li>{@code config/kalypzo-dev-tools/craftengine_images.json}, for ids the pack cannot tell:
 *       <pre>{@code
 *       { "default:bow": { "font": "minecraft:default", "char": "" } }
 *       }</pre></li>
 * </ol>
 */
public final class CraftEngineImages {

    public record Image(@NotNull FontDescription font, @NotNull String glyph) {}

    private static final CraftEngineImages INSTANCE = new CraftEngineImages();

    private final Path file = FabricLoader.getInstance().getConfigDir()
            .resolve("kalypzo-dev-tools").resolve("craftengine_images.json");

    //Keeps its order: the first id of a glyph is the one it is written back as
    private final Map<String, Image> fromFile = new LinkedHashMap<>();
    private final Map<String, Image> fromPacks = new LinkedHashMap<>();
    private long loadedModified = Long.MIN_VALUE;
    private @Nullable ResourceManager scannedManager = null;

    private CraftEngineImages(){}

    public static CraftEngineImages get(){
        return INSTANCE;
    }

    public Path file(){
        return file;
    }

    /**
     * @return null if no pack and not the file knows the id.
     */
    public synchronized @Nullable Image find(@NotNull String id){
        refresh();
        Image image = fromFile.get(id);
        return image != null ? image : fromPacks.get(id);
    }

    /**
     * The image that stands behind a glyph, the other direction of {@link #find}.
     *
     * @return null if no image looks like this.
     */
    public synchronized @Nullable String idOf(@NotNull FontDescription font, @NotNull String glyph){
        refresh();
        String id = idIn(fromFile, font, glyph);
        return id != null ? id : idIn(fromPacks, font, glyph);
    }

    private static @Nullable String idIn(Map<String, Image> images, FontDescription font, String glyph){
        for (Map.Entry<String, Image> entry : images.entrySet()) {
            if(entry.getValue().glyph().equals(glyph) && entry.getValue().font().equals(font)) return entry.getKey();
        }
        return null;
    }

    /**
     * All known image ids, sorted, for telling the AI what it may use.
     */
    public synchronized @NotNull List<String> ids(){
        refresh();
        List<String> ids = new ArrayList<>(fromFile.keySet());
        for (String id : fromPacks.keySet()) {
            if(!fromFile.containsKey(id)) ids.add(id);
        }
        ids.sort(null);
        return ids;
    }

    /**
     * How many images the active packs brought along.
     */
    public synchronized int packImageCount(){
        refresh();
        return fromPacks.size();
    }

    private void refresh(){
        refreshFile();
        refreshPacks();
    }

    // ------------------------------------------------------------------ resource packs

    private void refreshPacks(){
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        //A reload of the packs gives a new manager
        if(manager == scannedManager) return;

        scannedManager = manager;
        fromPacks.clear();

        try {
            Map<Identifier, List<Resource>> fonts = manager.listResourceStacks("font", id -> id.getPath().endsWith(".json"));
            for (Map.Entry<Identifier, List<Resource>> entry : fonts.entrySet()) {
                Identifier location = entry.getKey();
                String name = location.getPath().substring("font/".length(), location.getPath().length() - ".json".length());
                Identifier fontId = Identifier.fromNamespaceAndPath(location.getNamespace(), name);

                for (Resource resource : entry.getValue()) readFont(fontId, resource);
            }
        } catch (RuntimeException e) {
            FIMModClient.LOGGER.warn("Could not scan the resource packs for images", e);
        }
    }

    private void readFont(Identifier fontId, Resource resource){
        try (BufferedReader reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if(!root.has("providers")) return;

            for (JsonElement element : root.getAsJsonArray("providers")) {
                if(element.isJsonObject()) readProvider(fontId, element.getAsJsonObject());
            }
        } catch (IOException | RuntimeException e) {
            //A broken font of some pack is not our problem
            FIMModClient.LOGGER.debug("Skipped font {}", fontId, e);
        }
    }

    private void readProvider(Identifier fontId, JsonObject provider){
        String type = provider.has("type") ? provider.get("type").getAsString() : "";
        if(!type.equals("bitmap") && !type.equals("minecraft:bitmap")) return;
        if(!provider.has("file") || !provider.has("chars") || !provider.get("chars").isJsonArray()) return;

        Identifier texture = Identifier.tryParse(provider.get("file").getAsString());
        if(texture == null) return;

        JsonArray rows = provider.getAsJsonArray("chars");
        List<int[]> glyphs = new ArrayList<>();
        for (int row = 0; row < rows.size(); row++) {
            int[] codepoints = rows.get(row).getAsString().codePoints().toArray();
            for (int column = 0; column < codepoints.length; column++) {
                //Only private use glyphs are icons, the rest is vanilla's alphabet
                if(Character.getType(codepoints[column]) != Character.PRIVATE_USE) continue;
                glyphs.add(new int[]{codepoints[column], row, column});
            }
        }
        if(glyphs.isEmpty()) return;

        String base = idFromTexture(texture);
        FontDescription font = new FontDescription.Resource(fontId);

        for (int[] glyph : glyphs) {
            Image image = new Image(font, new String(Character.toChars(glyph[0])));
            String id = glyphs.size() == 1 ? base : base + ":" + glyph[1] + ":" + glyph[2];

            fromPacks.putIfAbsent(id, image);
            //The full texture path as well, for packs that do not follow the usual layout
            String full = texture.getNamespace() + ":" + stripPng(texture.getPath())
                    + (glyphs.size() == 1 ? "" : ":" + glyph[1] + ":" + glyph[2]);
            fromPacks.putIfAbsent(full, image);
        }
    }

    /**
     * {@code craftengine:font/image/default/bow.png} becomes {@code default:bow}, {@code ns:font/bow.png}
     * becomes {@code ns:bow}.
     */
    private static String idFromTexture(Identifier texture){
        String path = stripPng(texture.getPath());
        if(path.startsWith("font/")) path = path.substring("font/".length());
        if(path.startsWith("images/")) path = path.substring("images/".length());
        else if(path.startsWith("image/")) path = path.substring("image/".length());

        int slash = path.indexOf('/');
        if(slash < 0) return texture.getNamespace() + ":" + path;
        return path.substring(0, slash) + ":" + path.substring(slash + 1);
    }

    private static String stripPng(String path){
        return path.endsWith(".png") ? path.substring(0, path.length() - 4) : path;
    }

    // ------------------------------------------------------------------ file

    private void refreshFile(){
        long modified;
        try {
            if(!Files.exists(file)) create();
            modified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return;
        }

        if(modified == loadedModified) return;
        loadedModified = modified;
        fromFile.clear();

        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                Image image = read(entry.getValue());
                if(image != null) fromFile.put(entry.getKey(), image);
            }
        } catch (IOException | RuntimeException e) {
            FIMModClient.LOGGER.warn("Could not read {}", file, e);
        }
    }

    private static @Nullable Image read(JsonElement element){
        if(!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();

        String glyph = null;
        if(object.has("char")) glyph = object.get("char").getAsString();
        else if(object.has("chars") && object.get("chars").isJsonArray() && !object.getAsJsonArray("chars").isEmpty()){
            glyph = object.getAsJsonArray("chars").get(0).getAsString();
        }
        if(glyph == null || glyph.isEmpty()) return null;

        Identifier font = Identifier.tryParse(object.has("font") ? object.get("font").getAsString() : "minecraft:default");
        if(font == null) return null;

        return new Image(new FontDescription.Resource(font), glyph);
    }

    private void create() throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\n}\n", StandardCharsets.UTF_8);
    }

}
