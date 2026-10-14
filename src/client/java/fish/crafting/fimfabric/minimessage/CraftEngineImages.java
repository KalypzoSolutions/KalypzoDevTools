package fish.crafting.fimfabric.minimessage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fish.crafting.fimfabric.client.FIMModClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Knows what the images of a CraftEngine server look like on the client. The client never learns
 * the server's image ids, so they are listed in a file the user keeps next to the other configs:
 *
 * <pre>{@code
 * {
 *   "default:bow": { "font": "minecraft:default", "char": "" },
 *   "menu:icons": { "font": "menu:icons", "chars": [""] }
 * }
 * }</pre>
 *
 * The preview of the item editor turns {@code <image:default:bow>} into that glyph. The server
 * pack has to be active for the glyph to show up.
 */
public final class CraftEngineImages {

    public record Image(@NotNull FontDescription font, @NotNull String glyph) {}

    private static final CraftEngineImages INSTANCE = new CraftEngineImages();

    private final Path file = FabricLoader.getInstance().getConfigDir()
            .resolve("kalypzo-dev-tools").resolve("craftengine_images.json");
    private final Map<String, Image> images = new HashMap<>();
    private long loadedModified = Long.MIN_VALUE;

    private CraftEngineImages(){}

    public static CraftEngineImages get(){
        return INSTANCE;
    }

    public Path file(){
        return file;
    }

    /**
     * @return null if the id is not in the file.
     */
    public synchronized @Nullable Image find(@NotNull String id){
        reloadIfChanged();
        return images.get(id);
    }

    /**
     * The image that stands behind a glyph, the other direction of {@link #find}.
     *
     * @return null if no image of the file looks like this.
     */
    public synchronized @Nullable String idOf(@NotNull FontDescription font, @NotNull String glyph){
        reloadIfChanged();
        for (Map.Entry<String, Image> entry : images.entrySet()) {
            if(entry.getValue().glyph().equals(glyph) && entry.getValue().font().equals(font)) return entry.getKey();
        }
        return null;
    }

    /**
     * All known image ids, sorted, for telling the AI what it may use.
     */
    public synchronized @NotNull java.util.List<String> ids(){
        reloadIfChanged();
        return images.keySet().stream().sorted().toList();
    }

    private void reloadIfChanged(){
        long modified;
        try {
            if(!Files.exists(file)){
                create();
            }
            modified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return;
        }

        if(modified == loadedModified) return;
        loadedModified = modified;
        images.clear();

        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                Image image = read(entry.getValue());
                if(image != null) images.put(entry.getKey(), image);
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
