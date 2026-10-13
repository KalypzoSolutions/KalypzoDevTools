package fish.crafting.fimfabric.inspect.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.Window;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import fish.crafting.fimfabric.client.FIMModClient;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.multiplayer.ServerData;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/**
 * Reads what the client is showing right now. Has to run on the render thread.
 */
public class ViewCollector {

    public static ViewSnapshot collect(){
        Minecraft mc = Minecraft.getInstance();
        //Items and dialogs refer to registries the server sent, plain JSON ops cannot write those
        DynamicOps<JsonElement> ops = mc.level == null
                ? JsonOps.INSTANCE
                : mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        TextEncoder encoder = new TextEncoder(ops);

        JsonObject json = new JsonObject();
        json.add("meta", meta(mc));
        section(json, "screen", () -> ScreenCollector.collect(encoder, mc.gui.screen()));

        if(mc.level != null && mc.player != null){
            Hud hud = mc.gui.hud;
            section(json, "bossbars", () -> HudCollector.bossbars(encoder, hud));
            section(json, "title", () -> HudCollector.title(encoder, hud));
            section(json, "actionbar", () -> HudCollector.actionbar(encoder, hud));
            section(json, "scoreboard", () -> ScoreboardCollector.collect(encoder, mc.level.getScoreboard()));
            section(json, "tablist", () -> HudCollector.tablist(encoder, hud, mc.player.connection));
        }

        JsonObject fontsUsed = new JsonObject();
        encoder.getFontsUsed().forEach((font, places) -> {
            JsonArray array = new JsonArray();
            places.forEach(array::add);
            fontsUsed.add(font, array);
        });
        json.add("fontsUsed", fontsUsed);

        return new ViewSnapshot(label(mc), json, encoder.getFontsUsed());
    }

    /**
     * One part failing, say an item the codec refuses, should not cost the rest of the snapshot.
     */
    private static void section(JsonObject json, String name, Supplier<JsonElement> collector){
        try{
            json.add(name, collector.get());
        }catch (Exception e){
            FIMModClient.LOGGER.error("Could not collect '{}' for the view dump", name, e);
            JsonObject error = new JsonObject();
            error.addProperty("error", e.toString());
            json.add(name, error);
        }
    }

    private static JsonObject meta(Minecraft mc){
        JsonObject json = new JsonObject();
        json.addProperty("time", OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        json.addProperty("minecraft", SharedConstants.getCurrentVersion().name());
        json.addProperty("server", label(mc));

        Window window = mc.getWindow();
        JsonObject windowJson = new JsonObject();
        windowJson.addProperty("width", window.getWidth());
        windowJson.addProperty("height", window.getHeight());
        //Every coordinate in the snapshot is in these units, not in pixels
        windowJson.addProperty("guiWidth", window.getGuiScaledWidth());
        windowJson.addProperty("guiHeight", window.getGuiScaledHeight());
        windowJson.addProperty("guiScale", window.getGuiScale());
        json.add("window", windowJson);

        return json;
    }

    public static String label(Minecraft mc){
        ServerData server = mc.getCurrentServer();
        if(server != null) return server.ip;
        if(mc.hasSingleplayerServer()) return "singleplayer";
        return mc.level == null ? "menu" : "unknown";
    }

}
