package fish.crafting.fimfabric.inspect.dump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import fish.crafting.fimfabric.client.FIMModClient;
import fish.crafting.fimfabric.inspect.pack.PackReport;
import fish.crafting.fimfabric.inspect.pack.PackReport.*;
import fish.crafting.fimfabric.inspect.view.TextEncoder;
import fish.crafting.fimfabric.inspect.view.ViewSnapshot;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Lays a snapshot and a pack report out as a folder that can be handed to an LLM as it is.
 */
public class DumpWriter {

    public static final String ROOT = "fim-dumps";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Path directory;
    //Pack id -> its folder, relative to the dump
    private final Map<String, String> packDirectories = new HashMap<>();

    private DumpWriter(Path directory){
        this.directory = directory;
    }

    public static DumpWriter create(String label) throws IOException {
        Path root = Minecraft.getInstance().gameDirectory.toPath().resolve(ROOT);
        Path directory = root.resolve(STAMP.format(LocalDateTime.now()) + "_" + fileName(label));
        Files.createDirectories(directory);
        return new DumpWriter(directory);
    }

    public Path directory(){
        return directory;
    }

    public void writeView(ViewSnapshot snapshot) throws IOException {
        writeJson("view.json", snapshot.json());
    }

    public void writeScreenshot(NativeImage image) throws IOException {
        image.writeToFile(directory.resolve("screenshot.png"));
    }

    public void writeReadme(@Nullable ViewSnapshot snapshot, PackReport report) throws IOException {
        Files.writeString(directory.resolve("README.md"), DumpReadme.build(snapshot, report), StandardCharsets.UTF_8);
    }

    /**
     * Copies the fonts and shaders of the report and writes the index that says where each of them went.
     */
    public void writePacks(PackReport report) throws IOException {
        JsonObject index = new JsonObject();

        JsonArray packs = new JsonArray();
        for (PackInfo pack : report.packs()) {
            JsonObject json = new JsonObject();
            json.addProperty("id", pack.id());
            json.addProperty("title", pack.title());
            json.addProperty("source", pack.source());
            json.addProperty("directory", packDirectory(pack.id()));
            //Uncompressed, the zip the server hands out is smaller
            json.addProperty("bytes", pack.bytes());
            json.addProperty("files", pack.files());
            JsonObject categories = new JsonObject();
            pack.bytesByCategory().forEach(categories::addProperty);
            json.add("bytesByCategory", categories);
            packs.add(json);
        }
        index.add("packs", packs);

        JsonArray fonts = new JsonArray();
        for (FontInfo font : report.fonts()) {
            JsonObject json = new JsonObject();
            json.addProperty("id", font.id().toString());

            JsonArray definitions = new JsonArray();
            for (FontDefinition definition : font.definitions()) {
                JsonObject definitionJson = new JsonObject();
                definitionJson.addProperty("pack", definition.file().packId());
                definitionJson.addProperty("file", copy(definition.file()));
                if(definition.error() != null) definitionJson.addProperty("error", definition.error());

                JsonArray providers = new JsonArray();
                for (FontProvider provider : definition.providers()) {
                    providers.add(provider(provider));
                }
                definitionJson.add("providers", providers);
                definitions.add(definitionJson);
            }
            json.add("definitions", definitions);
            fonts.add(json);
        }
        index.add("fonts", fonts);

        JsonArray shaders = new JsonArray();
        for (ShaderInfo shader : report.shaders()) {
            JsonObject json = new JsonObject();
            json.addProperty("id", shader.location().toString());
            json.addProperty("kind", shader.kind());
            json.addProperty("pack", shader.file().packId());
            json.addProperty("active", shader.active());
            json.addProperty("file", copy(shader.file()));
            json.addProperty("vanilla", shader.vanilla() == null ? null : copy(shader.vanilla(), "vanilla"));
            shaders.add(json);
        }
        index.add("shaders", shaders);

        writeJson("packs.json", index);
    }

    private JsonObject provider(FontProvider provider){
        JsonObject json = new JsonObject();
        json.addProperty("type", provider.type());
        if(provider.reference() != null) json.addProperty("reference", provider.reference().toString());
        if(provider.referenced() != null) json.addProperty("file", copy(provider.referenced()));

        if(provider.type().equals("bitmap")){
            json.addProperty("height", provider.height());
            json.addProperty("ascent", provider.ascent());

            JsonArray codepoints = new JsonArray();
            for (int codepoint : provider.codepoints()) {
                //Empty cells of the grid
                if(codepoint != 0) codepoints.add(TextEncoder.codepoint(codepoint));
            }
            json.add("codepoints", codepoints);
        }

        if(!provider.advances().isEmpty()){
            JsonObject advances = new JsonObject();
            provider.advances().forEach((codepoint, advance) -> advances.addProperty(TextEncoder.codepoint(codepoint), advance));
            json.add("advances", advances);
        }

        return json;
    }

    private @Nullable String copy(PackFile file){
        return copy(file, packDirectory(file.packId()));
    }

    /**
     * @return Where the file went, relative to the dump, or null if it could not be read
     */
    private @Nullable String copy(PackFile file, String base){
        String relative = base + "/" + file.assetPath();
        Path target = directory.resolve(relative).normalize();
        //The path comes out of a pack somebody else made, "../" is a valid part of it
        if(!target.startsWith(directory.resolve(base))) return null;

        try(InputStream in = file.content().get()){
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return relative;
        }catch (IOException e){
            FIMModClient.LOGGER.warn("Could not copy {} of {} into the dump", file.location(), file.packId(), e);
            return null;
        }
    }

    private String packDirectory(String packId){
        return packDirectories.computeIfAbsent(packId, id -> "packs/" + (packDirectories.size() + 1) + "-" + fileName(id));
    }

    private void writeJson(String name, JsonElement json) throws IOException {
        Files.writeString(directory.resolve(name), escapeNonAscii(GSON.toJson(json)), StandardCharsets.UTF_8);
    }

    /**
     * Private use characters are what custom fonts are made of, and most of them show up as nothing
     * or as a box. Outside of strings JSON is ASCII anyway, so this only ever touches string contents.
     */
    private static String escapeNonAscii(String json){
        StringBuilder out = new StringBuilder(json.length());
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if(c > 0x7E) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.toString();
    }

    private static String fileName(String name){
        String cleaned = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }

}
