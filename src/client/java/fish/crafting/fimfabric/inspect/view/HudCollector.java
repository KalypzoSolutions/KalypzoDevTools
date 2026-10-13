package fish.crafting.fimfabric.inspect.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import fish.crafting.fimfabric.mixin.BossHealthOverlayAccessor;
import fish.crafting.fimfabric.mixin.HudAccessor;
import fish.crafting.fimfabric.mixin.PlayerTabOverlayAccessor;
import net.minecraft.Optionull;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Comparator;
import java.util.List;

public class HudCollector {

    //Vanilla shows no more than this, in this order
    private static final int MAX_TAB_ENTRIES = 80;
    private static final Comparator<PlayerInfo> TAB_ORDER = Comparator.<PlayerInfo>comparingInt(p -> -p.getTabListOrder())
            .thenComparingInt(p -> p.getGameMode() == GameType.SPECTATOR ? 1 : 0)
            .thenComparing(p -> Optionull.mapOrDefault(p.getTeam(), PlayerTeam::getName, ""))
            .thenComparing(p -> p.getProfile().name(), String::compareToIgnoreCase);

    /**
     * In the order they are stacked from the top of the screen.
     */
    public static JsonArray bossbars(TextEncoder encoder, Hud hud){
        JsonArray bars = new JsonArray();

        for (LerpingBossEvent event : ((BossHealthOverlayAccessor) hud.getBossOverlay()).getFimEvents().values()) {
            JsonObject json = new JsonObject();
            json.addProperty("id", event.getId().toString());
            json.add("name", encoder.text(event.getName(), "bossbars[" + bars.size() + "].name"));
            json.addProperty("progress", event.getProgress());
            json.addProperty("color", event.getColor().getSerializedName());
            json.addProperty("overlay", event.getOverlay().getSerializedName());
            json.addProperty("darkenScreen", event.shouldDarkenScreen());
            json.addProperty("playBossMusic", event.shouldPlayBossMusic());
            json.addProperty("createWorldFog", event.shouldCreateWorldFog());
            bars.add(json);
        }

        return bars;
    }

    public static JsonElement title(TextEncoder encoder, Hud hud){
        HudAccessor accessor = (HudAccessor) hud;
        if(accessor.getFimTitle() == null || accessor.getFimTitleTime() <= 0) return JsonNull.INSTANCE;

        JsonObject json = new JsonObject();
        json.add("title", encoder.text(accessor.getFimTitle(), "title.title"));
        json.add("subtitle", encoder.text(accessor.getFimSubtitle(), "title.subtitle"));
        json.addProperty("fadeInTicks", accessor.getFimTitleFadeInTime());
        json.addProperty("stayTicks", accessor.getFimTitleStayTime());
        json.addProperty("fadeOutTicks", accessor.getFimTitleFadeOutTime());
        json.addProperty("remainingTicks", accessor.getFimTitleTime());
        return json;
    }

    public static JsonElement actionbar(TextEncoder encoder, Hud hud){
        HudAccessor accessor = (HudAccessor) hud;
        if(accessor.getFimOverlayMessage() == null || accessor.getFimOverlayMessageTime() <= 0) return JsonNull.INSTANCE;

        JsonObject json = new JsonObject();
        json.add("text", encoder.text(accessor.getFimOverlayMessage(), "actionbar.text"));
        json.addProperty("remainingTicks", accessor.getFimOverlayMessageTime());
        //Only the "now playing" message of a jukebox cycles through colors
        json.addProperty("animatedColor", accessor.getFimAnimateOverlayMessageColor());
        return json;
    }

    public static JsonObject tablist(TextEncoder encoder, Hud hud, ClientPacketListener connection){
        PlayerTabOverlay tabList = hud.getTabList();
        PlayerTabOverlayAccessor accessor = (PlayerTabOverlayAccessor) tabList;

        JsonObject json = new JsonObject();
        json.add("header", encoder.text(accessor.getFimHeader(), "tablist.header"));
        json.add("footer", encoder.text(accessor.getFimFooter(), "tablist.footer"));

        List<PlayerInfo> infos = connection.getListedOnlinePlayers().stream().sorted(TAB_ORDER).toList();
        json.addProperty("playerCount", infos.size());

        JsonArray players = new JsonArray();
        for (PlayerInfo info : infos) {
            if(players.size() >= MAX_TAB_ENTRIES) break;

            JsonObject player = new JsonObject();
            player.addProperty("name", info.getProfile().name());
            player.add("display", encoder.text(tabList.getNameForDisplay(info), "tablist.players[" + players.size() + "].display"));
            player.addProperty("latency", info.getLatency());
            player.addProperty("order", info.getTabListOrder());
            players.add(player);
        }
        json.add("players", players);

        return json;
    }

}
