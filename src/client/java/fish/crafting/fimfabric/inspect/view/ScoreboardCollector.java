package fish.crafting.fimfabric.inspect.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.*;

import java.util.Comparator;
import java.util.List;

public class ScoreboardCollector {

    //The sidebar has room for this many lines, anything below is never drawn
    private static final int MAX_SIDEBAR_LINES = 15;
    private static final int MAX_OTHER_ENTRIES = 80;
    private static final Comparator<PlayerScoreEntry> DISPLAY_ORDER = Comparator.comparing(PlayerScoreEntry::value)
            .reversed()
            .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

    /**
     * One entry per display slot that has an objective: "sidebar", "list", "below_name" and the team sidebars.
     */
    public static JsonObject collect(TextEncoder encoder, Scoreboard scoreboard){
        JsonObject json = new JsonObject();

        for (DisplaySlot slot : DisplaySlot.values()) {
            Objective objective = scoreboard.getDisplayObjective(slot);
            if(objective == null) continue;

            json.add(slot.getSerializedName(), objective(encoder, scoreboard, objective, slot));
        }

        return json;
    }

    private static JsonObject objective(TextEncoder encoder, Scoreboard scoreboard, Objective objective, DisplaySlot slot){
        String where = "scoreboard." + slot.getSerializedName();
        boolean list = slot == DisplaySlot.LIST;
        boolean sidebar = !list && slot != DisplaySlot.BELOW_NAME;

        JsonObject json = new JsonObject();
        json.addProperty("objective", objective.getName());
        json.add("displayName", encoder.text(objective.getDisplayName(), where + ".displayName"));
        json.addProperty("renderType", objective.getRenderType().getSerializedName());
        if(objective.numberFormat() != null){
            json.add("numberFormat", encoder.encode(NumberFormatTypes.CODEC, objective.numberFormat()));
        }

        NumberFormat format = objective.numberFormatOrDefault(list ? StyledFormat.PLAYER_LIST_DEFAULT : StyledFormat.SIDEBAR_DEFAULT);
        List<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective).stream()
                .filter(score -> !score.isHidden())
                .sorted(DISPLAY_ORDER)
                .limit(sidebar ? MAX_SIDEBAR_LINES : MAX_OTHER_ENTRIES)
                .toList();

        JsonArray entries = new JsonArray();
        for (PlayerScoreEntry score : scores) {
            String entryWhere = where + ".entries[" + entries.size() + "]";
            PlayerTeam team = scoreboard.getPlayersTeam(score.owner());

            JsonObject entry = new JsonObject();
            entry.addProperty("owner", score.owner());
            entry.addProperty("value", score.value());
            //What ends up on screen: the owner or its display name, wrapped in the team's prefix and suffix
            entry.add("name", encoder.text(PlayerTeam.formatNameForTeam(team, score.ownerName()), entryWhere + ".name"));
            entry.add("score", encoder.text(score.formatValue(format), entryWhere + ".score"));
            if(score.display() != null){
                entry.add("display", encoder.encode(ComponentSerialization.CODEC, score.display()));
            }
            if(score.numberFormatOverride() != null){
                entry.add("numberFormat", encoder.encode(NumberFormatTypes.CODEC, score.numberFormatOverride()));
            }
            if(team != null) entry.add("team", team(encoder, team, entryWhere + ".team"));

            entries.add(entry);
        }
        json.add("entries", entries);

        return json;
    }

    private static JsonObject team(TextEncoder encoder, PlayerTeam team, String where){
        JsonObject json = new JsonObject();
        json.addProperty("name", team.getName());
        json.add("prefix", encoder.text(team.getPlayerPrefix(), where + ".prefix"));
        json.add("suffix", encoder.text(team.getPlayerSuffix(), where + ".suffix"));
        team.getColor().ifPresent(color -> json.addProperty("color", color.getSerializedName()));
        return json;
    }

}
