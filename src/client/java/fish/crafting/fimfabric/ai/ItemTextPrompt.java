package fish.crafting.fimfabric.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the prompt for rewriting an item's name and lore in MiniMessage, and reads the answer.
 */
public final class ItemTextPrompt {

    private static final Pattern NAME = Pattern.compile("(?is)<name>(.*?)</name>");
    private static final Pattern LORE = Pattern.compile("(?is)<lore>(.*?)</lore>");

    private ItemTextPrompt(){}

    /**
     * @param name The new name, null if the answer did not have one.
     * @param lore The new lore, null if the answer did not have one.
     */
    public record Answer(@Nullable String name, @Nullable String lore) {
        public boolean isEmpty(){
            return name == null && lore == null;
        }
    }

    public static @NotNull String build(@NotNull String itemId, @NotNull String name, @NotNull String lore, @NotNull String instruction){
        return """
                You rewrite the display name and lore of a Minecraft item. Reply with the result only.

                Item: %s

                Use MiniMessage for formatting. Supported tags: <red> <gold> <#rrggbb> <color:...>,
                <bold> <italic> <underlined> <strikethrough> <obfuscated>, <!italic> to switch italics off,
                <gradient:#rrggbb:#rrggbb> <rainbow> <reset>. Lore lines are separated by a line break.
                Lore is italic by default, so start lines with <!italic> unless italics are wanted.
                Do not use click, hover or font tags.

                Reply in exactly this format, with no other text and no code fences:
                <name>the name</name>
                <lore>
                first lore line
                second lore line
                </lore>

                Current name:
                %s

                Current lore:
                %s

                What to change:
                %s
                """.formatted(itemId,
                name.isBlank() ? "(none)" : name,
                lore.isBlank() ? "(none)" : lore,
                instruction.isBlank() ? "Improve it and make it look good." : instruction);
    }

    public static @NotNull Answer parse(@NotNull String reply){
        return new Answer(find(NAME, reply), find(LORE, reply));
    }

    private static @Nullable String find(Pattern pattern, String text){
        Matcher matcher = pattern.matcher(text);
        if(!matcher.find()) return null;
        return matcher.group(1).strip();
    }

}
