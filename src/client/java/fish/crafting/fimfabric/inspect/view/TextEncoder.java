package fish.crafting.fimfabric.inspect.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Writes a text twice. Once as the component the server sent, ready to be sent again, and once
 * broken into styled runs with the advance of every character. The second form is what makes
 * negative space and colors that only a shader reads visible.
 */
@RequiredArgsConstructor
public class TextEncoder {

    private final DynamicOps<JsonElement> ops;
    //Font -> places it is used at
    @Getter
    private final Map<String, Set<String>> fontsUsed = new TreeMap<>();

    /**
     * @param where Path of the text inside the snapshot, remembered for every font it uses
     */
    public JsonElement text(@Nullable Component text, String where){
        if(text == null) return JsonNull.INSTANCE;

        JsonObject json = new JsonObject();
        json.add("component", encode(ComponentSerialization.CODEC, text));
        json.addProperty("plain", text.getString());

        StringSplitter splitter = Minecraft.getInstance().font.getSplitter();
        JsonArray runs = new JsonArray();
        float[] width = new float[1];

        text.visit((style, content) -> {
            if(content.isEmpty()) return Optional.empty();

            String font = fontName(style.getFont());
            fontsUsed.computeIfAbsent(font, key -> new TreeSet<>()).add(where);

            JsonObject run = new JsonObject();
            run.addProperty("text", content);
            run.addProperty("font", font);
            writeStyle(run, style);

            JsonArray codepoints = new JsonArray();
            JsonArray advances = new JsonArray();
            content.codePoints().forEach(codepoint -> {
                float advance = splitter.stringWidth(FormattedCharSequence.codepoint(codepoint, style));
                codepoints.add(codepoint(codepoint));
                advances.add(advance);
                width[0] += advance;
            });
            run.add("codepoints", codepoints);
            run.add("advances", advances);

            runs.add(run);
            return Optional.empty();
        }, Style.EMPTY);

        json.addProperty("width", width[0]);
        json.add("runs", runs);
        return json;
    }

    /**
     * Anything the game has a codec for comes out as the JSON a server or datapack would use for it.
     */
    public <T> JsonElement encode(Codec<T> codec, T value){
        return codec.encodeStart(ops, value).mapOrElse(json -> json, error -> {
            JsonObject json = new JsonObject();
            json.addProperty("error", error.message());
            return json;
        });
    }

    private static void writeStyle(JsonObject run, Style style){
        TextColor color = style.getColor();
        if(color != null) run.addProperty("color", String.format("#%06X", color.getValue()));

        Integer shadowColor = style.getShadowColor();
        if(shadowColor != null) run.addProperty("shadowColor", String.format("#%08X", shadowColor));

        if(style.isBold()) run.addProperty("bold", true);
        if(style.isItalic()) run.addProperty("italic", true);
        if(style.isUnderlined()) run.addProperty("underlined", true);
        if(style.isStrikethrough()) run.addProperty("strikethrough", true);
        if(style.isObfuscated()) run.addProperty("obfuscated", true);
    }

    private static String fontName(FontDescription font){
        return switch (font) {
            case FontDescription.Resource resource -> resource.id().toString();
            case FontDescription.AtlasSprite sprite -> "sprite:" + sprite.atlasId() + "/" + sprite.spriteId();
            case FontDescription.PlayerSprite ignored -> "player_sprite";
            default -> font.toString();
        };
    }

    public static String codepoint(int codepoint){
        return String.format("U+%04X", codepoint);
    }

}
