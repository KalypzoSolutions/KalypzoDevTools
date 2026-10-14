package fish.crafting.fimfabric.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the prompt for rewriting an item's name and lore in MiniMessage, and reads the answer.
 * The answer is a JSON object, which is what makes it safe to put back into the editor.
 */
public final class ItemTextPrompt {

    private ItemTextPrompt(){}

    /**
     * @param name The new name, null if the AI leaves it as it is.
     * @param lore The new lore lines, null if the AI leaves them as they are.
     * @param notes A remark of the AI for the user, may be null.
     */
    public record Answer(@Nullable String name, @Nullable List<String> lore, @Nullable String notes) {
        public boolean changesAnything(){
            return name != null || lore != null;
        }
    }

    /**
     * @param itemData Everything known about the item, one fact per line.
     * @param name Current name as MiniMessage, may be empty.
     * @param lore Current lore as MiniMessage, one line per row, may be empty.
     * @param task What the user wants.
     * @param imageIds CraftEngine images the AI may use as {@code <image:id>}, may be empty.
     */
    public static @NotNull String build(@NotNull String itemData, @NotNull String name, @NotNull String lore, @NotNull String task, @NotNull List<String> imageIds){
        return """
                You edit the display name and the lore of a Minecraft item (Java Edition). Text is written in
                MiniMessage. You only answer with the JSON object described below.

                == Item ==
                %s

                == Current name (MiniMessage) ==
                %s

                == Current lore (MiniMessage, one line per row) ==
                %s

                == Task ==
                %s

                == MiniMessage you may use ==
                Colors: <red> <gold> <yellow> <green> <aqua> <blue> <light_purple> <gray> ... or <#rrggbb> or <color:#rrggbb>
                Decorations: <bold> <italic> <underlined> <strikethrough> <obfuscated>, switched off with <!italic>
                Effects: <gradient:#rrggbb:#rrggbb> <rainbow>, <reset> clears everything
                Names and lore are shown in italics by default, so start a line with <!italic> unless italics are wanted.
                Icons of a CraftEngine server are written as <image:namespace:id>, text in a custom font as
                <font:namespace:path>. A character that cannot be typed is written as <glyph:HEX>, for example <glyph:E001>.
                Keep every existing <image:...>, <glyph:...> and <font:...> tag exactly as it is, unless
                the task asks to change it. Never turn an image into a plain character.
                %s
                Do not use click, hover or other tags. Keep lore lines short enough for a tooltip.

                == Answer format ==
                Answer with exactly one JSON object and nothing else, no code fence, no text around it:
                {"name": <string or null>, "lore": <array of strings or null>, "notes": <string or null>}
                - "name": the new name as MiniMessage, or null to keep the current name.
                - "lore": the new lore as an array with one MiniMessage string per line, or null to keep the
                  current lore. An empty string is an empty line. An empty array removes the lore.
                - "notes": one short sentence for the user, or null.
                """.formatted(
                itemData.isBlank() ? "(nothing known)" : itemData,
                name.isBlank() ? "(none)" : name,
                lore.isBlank() ? "(none)" : lore,
                task.isBlank() ? "Improve the name and lore and make them look good." : task,
                imageIds.isEmpty() ? "No image ids are known, so do not invent any <image:...> tags."
                        : "Images you may use: " + String.join(", ", imageIds.stream().limit(300).toList()));
    }

    /**
     * @throws IllegalArgumentException with a message for the user if the reply is not usable.
     */
    public static @NotNull Answer parse(@NotNull String reply){
        //Tools like to wrap the answer in a code fence or a sentence
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if(start < 0 || end <= start) throw new IllegalArgumentException("The answer contained no JSON object");

        JsonObject json;
        try {
            JsonElement parsed = JsonParser.parseString(reply.substring(start, end + 1));
            if(!parsed.isJsonObject()) throw new IllegalArgumentException("The answer is not a JSON object");
            json = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The answer is not valid JSON: " + e.getMessage(), e);
        }

        String name = text(json, "name");

        List<String> lore = null;
        JsonElement loreElement = json.get("lore");
        if(loreElement != null && loreElement.isJsonArray()){
            lore = new ArrayList<>();
            JsonArray array = loreElement.getAsJsonArray();
            for (JsonElement line : array) lore.add(line.isJsonNull() ? "" : line.getAsString());
        } else if(loreElement != null && loreElement.isJsonPrimitive()){
            //One string with line breaks is close enough to an array
            lore = List.of(loreElement.getAsString().split("\n", -1));
        }

        return new Answer(name, lore, text(json, "notes"));
    }

    private static @Nullable String text(JsonObject json, String key){
        JsonElement element = json.get(key);
        if(element == null || !element.isJsonPrimitive()) return null;
        return element.getAsString();
    }

}
