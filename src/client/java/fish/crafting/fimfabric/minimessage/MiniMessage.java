package fish.crafting.fimfabric.minimessage;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A small MiniMessage implementation that works without the Adventure library. It understands the
 * tags that matter for items: colors, decorations, gradient, rainbow, reset, newline, key and lang.
 * Everything it does not know stays in the text untouched, like the real thing does.
 */
public final class MiniMessage {

    private MiniMessage(){}

    /**
     * @param lines One component per line, ready to be used as a name or as lore.
     * @param warnings Things worth telling the user about, like unknown or unclosed tags.
     */
    public record Result(@NotNull List<Component> lines, @NotNull List<String> warnings) {
        /**
         * All lines in one component, for a name.
         */
        public Component joined(){
            MutableComponent out = Component.empty();
            for (int i = 0; i < lines.size(); i++) {
                if(i > 0) out.append("\n");
                out.append(lines.get(i));
            }
            return out;
        }
    }

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("b", "bold"),
            Map.entry("i", "italic"),
            Map.entry("em", "italic"),
            Map.entry("u", "underlined"),
            Map.entry("st", "strikethrough"),
            Map.entry("obf", "obfuscated"),
            Map.entry("c", "color"),
            Map.entry("colour", "color"),
            Map.entry("br", "newline"),
            Map.entry("lang", "translatable"),
            Map.entry("tr", "translatable")
    );

    private static final Set<String> DECORATIONS = Set.of("bold", "italic", "underlined", "strikethrough", "obfuscated");
    private static final Set<String> OTHER_TAGS = Set.of("color", "gradient", "rainbow", "reset", "newline", "key", "translatable", "font", "image", "shift", "glyph");

    private static final class Frame {
        final String name;
        final List<TextColor> colors = new ArrayList<>();
        boolean value = true;
        //For the font tag
        @Nullable FontDescription font = null;
        //Gradient and rainbow need to know how long the text inside of them is
        int total = 0;
        int seen = 0;

        Frame(String name){
            this.name = name;
        }

        boolean spreads(){
            return name.equals("gradient") || name.equals("rainbow");
        }
    }

    private record Run(String text, List<Frame> frames, Component special) {}

    /**
     * Every line of the input becomes at least one line of the result.
     */
    public static Result parse(String input){
        List<String> warnings = new ArrayList<>();
        List<Component> lines = new ArrayList<>();

        for (String line : input.split("\n", -1)) {
            lines.addAll(parseLine(line, warnings));
        }

        return new Result(lines, warnings);
    }

    private static List<Component> parseLine(String input, List<String> warnings){
        List<Frame> open = new ArrayList<>();
        List<Run> runs = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        int i = 0;
        while(i < input.length()){
            char c = input.charAt(i);

            if(c == '\\' && i + 1 < input.length() && (input.charAt(i + 1) == '<' || input.charAt(i + 1) == '\\')){
                text.append(input.charAt(i + 1));
                i += 2;
                continue;
            }

            int end = c == '<' ? input.indexOf('>', i) : -1;
            if(end < 0){
                text.append(c);
                i++;
                continue;
            }

            String raw = input.substring(i + 1, end);
            if(!handleTag(raw, open, runs, text, warnings)){
                //Not a tag we know, so it is just text
                text.append(input, i, end + 1);
            }
            i = end + 1;
        }

        flush(text, open, runs);
        //Tags that are never closed are fine, they simply last until the end of the line

        return render(runs);
    }

    private static void flush(StringBuilder text, List<Frame> open, List<Run> runs){
        if(text.isEmpty()) return;

        String content = text.toString();
        text.setLength(0);

        int length = content.codePointCount(0, content.length());
        for (Frame frame : open) frame.total += length;
        runs.add(new Run(content, new ArrayList<>(open), null));
    }

    /**
     * @return false if the tag is not one of ours and should stay in the text.
     */
    private static boolean handleTag(String raw, List<Frame> open, List<Run> runs, StringBuilder text, List<String> warnings){
        if(raw.isEmpty()) return false;

        boolean closing = raw.startsWith("/");
        boolean negated = raw.startsWith("!");
        String body = closing || negated ? raw.substring(1) : raw;

        String[] parts = body.split(":");
        String name = parts[0].toLowerCase(Locale.ROOT);
        name = ALIASES.getOrDefault(name, name);

        //<red>, <#ff8800>
        boolean plainColor = name.equals("color") || name.startsWith("#") || (!name.equals("reset") && TextColor.parseColor(name).result().isPresent());
        if(plainColor) name = "color";
        if(!plainColor && !DECORATIONS.contains(name) && !OTHER_TAGS.contains(name)) return false;

        if(closing){
            int index = -1;
            for (int j = open.size() - 1; j >= 0; j--) {
                if(open.get(j).name.equals(name)){
                    index = j;
                    break;
                }
            }

            if(index < 0){
                warnings.add("</" + name + "> closes nothing");
                return true;
            }

            flush(text, open, runs);
            //Closing a tag closes everything that was opened inside of it as well
            open.subList(index, open.size()).clear();
            return true;
        }

        flush(text, open, runs);

        if(plainColor){
            String value = parts[0].toLowerCase(Locale.ROOT);
            //<color:red> keeps its value one part further
            if(ALIASES.getOrDefault(value, value).equals("color")) {
                if(parts.length < 2) return false;
                value = parts[1];
            }

            Optional<TextColor> color = TextColor.parseColor(value).result();
            if(color.isEmpty()){
                warnings.add("Unknown color " + value);
                return true;
            }

            Frame frame = new Frame("color");
            frame.colors.add(color.get());
            open.add(frame);
            return true;
        }

        switch (name) {
            case "reset" -> open.clear();
            case "newline" -> {
                for (Frame frame : open) frame.total += 1;
                runs.add(new Run("\n", new ArrayList<>(open), null));
            }
            case "key" -> {
                if(parts.length < 2) return false;
                runs.add(new Run("", new ArrayList<>(open), Component.keybind(parts[1])));
            }
            case "translatable" -> {
                if(parts.length < 2) return false;
                runs.add(new Run("", new ArrayList<>(open), Component.translatable(parts[1])));
            }
            case "gradient" -> {
                Frame frame = new Frame("gradient");
                for (int j = 1; j < parts.length; j++) {
                    Optional<TextColor> color = TextColor.parseColor(parts[j]).result();
                    if(color.isPresent()) frame.colors.add(color.get());
                    else warnings.add("Unknown gradient color " + parts[j]);
                }
                //No colors given means white to black in the real thing
                if(frame.colors.isEmpty()){
                    frame.colors.add(TextColor.fromRgb(0xFFFFFF));
                    frame.colors.add(TextColor.fromRgb(0x000000));
                }
                open.add(frame);
            }
            case "rainbow" -> open.add(new Frame("rainbow"));
            case "font" -> {
                //The font id has a colon of its own: <font:namespace:path>
                Identifier id = parts.length < 2 ? null : Identifier.tryParse(String.join(":", java.util.Arrays.copyOfRange(parts, 1, parts.length)));
                if(id == null){
                    warnings.add("<font> needs a font id, like <font:minecraft:default>");
                    return true;
                }

                Frame frame = new Frame("font");
                frame.font = new FontDescription.Resource(id);
                open.add(frame);
            }
            case "image" -> {
                //CraftEngine: <image:namespace:id>
                String id = String.join(":", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                CraftEngineImages.Image image = CraftEngineImages.get().find(id);
                if(image == null){
                    warnings.add("Unknown image " + id + " - add it to " + CraftEngineImages.get().file().getFileName());
                    return false;
                }
                runs.add(new Run("", new ArrayList<>(open), Component.literal(image.glyph())
                        .withStyle(Style.EMPTY.withFont(image.font()))));
            }
            //Shifts need the negative space font of the server's pack, there is nothing to preview
            case "shift" -> {}
            //A character that cannot be shown or typed, written as its code point: <glyph:E001>
            case "glyph" -> {
                int codepoint;
                try {
                    codepoint = parts.length < 2 ? -1 : Integer.parseInt(parts[1], 16);
                } catch (NumberFormatException e) {
                    codepoint = -1;
                }
                if(!Character.isValidCodePoint(codepoint)) return false;

                //It belongs to the text around it, so it takes the style of the open tags
                text.appendCodePoint(codepoint);
            }
            default -> {
                //Only the decorations are left
                Frame frame = new Frame(name);
                //<!bold> and <bold:false> both switch it off
                frame.value = !negated && !(parts.length > 1 && parts[1].equalsIgnoreCase("false"));
                open.add(frame);
            }
        }
        return true;
    }

    private static List<Component> render(List<Run> runs){
        List<Component> lines = new ArrayList<>();
        MutableComponent current = Component.empty();
        StringBuilder segment = new StringBuilder();

        for (Run run : runs) {
            if(run.special() != null){
                current.append(run.special().copy().withStyle(styleOf(run.frames(), false)));
                continue;
            }

            boolean perCharacter = run.frames().stream().anyMatch(Frame::spreads);

            for (int codepoint : run.text().codePoints().toArray()) {
                if(codepoint == '\n'){
                    emit(current, run, segment);
                    advance(run);
                    lines.add(current);
                    current = Component.empty();
                } else if(perCharacter){
                    current.append(Component.literal(new String(Character.toChars(codepoint)))
                            .withStyle(styleOf(run.frames(), true)));
                    advance(run);
                } else {
                    segment.appendCodePoint(codepoint);
                    advance(run);
                }
            }
            emit(current, run, segment);
        }

        lines.add(current);
        return lines;
    }

    private static void advance(Run run){
        for (Frame frame : run.frames()) frame.seen++;
    }

    private static void emit(MutableComponent current, Run run, StringBuilder segment){
        if(segment.isEmpty()) return;
        current.append(Component.literal(segment.toString()).withStyle(styleOf(run.frames(), false)));
        segment.setLength(0);
    }

    private static Style styleOf(List<Frame> frames, boolean spread){
        Style style = Style.EMPTY;
        for (Frame frame : frames) {
            switch (frame.name) {
                case "color" -> style = style.withColor(frame.colors.getFirst());
                case "font" -> style = style.withFont(frame.font);
                case "bold" -> style = style.withBold(frame.value);
                case "italic" -> style = style.withItalic(frame.value);
                case "underlined" -> style = style.withUnderlined(frame.value);
                case "strikethrough" -> style = style.withStrikethrough(frame.value);
                case "obfuscated" -> style = style.withObfuscated(frame.value);
                case "gradient" -> {
                    if(spread) style = style.withColor(gradient(frame.colors, position(frame)));
                }
                case "rainbow" -> {
                    if(spread) style = style.withColor(TextColor.fromRgb(Color.HSBtoRGB((float) position(frame), 1f, 1f) & 0xFFFFFF));
                }
                default -> {}
            }
        }
        return style;
    }

    private static double position(Frame frame){
        return frame.total <= 1 ? 0 : (double) frame.seen / (frame.total - 1);
    }

    private static TextColor gradient(List<TextColor> colors, double t){
        if(colors.size() == 1) return colors.getFirst();

        double scaled = Math.clamp(t, 0, 1) * (colors.size() - 1);
        int index = Math.min((int) scaled, colors.size() - 2);
        double local = scaled - index;

        int a = colors.get(index).getValue();
        int b = colors.get(index + 1).getValue();
        int r = lerp((a >> 16) & 0xFF, (b >> 16) & 0xFF, local);
        int g = lerp((a >> 8) & 0xFF, (b >> 8) & 0xFF, local);
        int bl = lerp(a & 0xFF, b & 0xFF, local);
        return TextColor.fromRgb((r << 16) | (g << 8) | bl);
    }

    private static int lerp(int a, int b, double t){
        return (int) Math.round(a + (b - a) * t);
    }

    // ---------------------------------------------------------------- the other way around

    /**
     * Writes private use characters as {@code <glyph:HEX>}, the way {@link #serialize} does, for text
     * that is not a component, like the raw component data shown to an AI.
     */
    public static String protectGlyphs(String text){
        StringBuilder out = new StringBuilder();
        for (int codepoint : text.codePoints().toArray()) {
            if(Character.getType(codepoint) == Character.PRIVATE_USE){
                out.append("<glyph:").append(Integer.toHexString(codepoint).toUpperCase(Locale.ROOT)).append('>');
            } else {
                out.appendCodePoint(codepoint);
            }
        }
        return out.toString();
    }

    /**
     * How many icons (images and glyphs) a text contains.
     */
    public static int countIcons(String text){
        int count = 0;
        for (String marker : new String[]{"<glyph:", "<image:"}) {
            for (int at = text.indexOf(marker); at >= 0; at = text.indexOf(marker, at + 1)) count++;
        }
        return count;
    }

    /**
     * Turns a component back into MiniMessage, so existing names and lore can be edited.
     */
    public static String serialize(Component component){
        StringBuilder out = new StringBuilder();
        Style[] previous = {Style.EMPTY};

        component.visit((style, content) -> {
            if(content.isEmpty()) return Optional.empty();

            //Icons of a CraftEngine server are glyphs of a custom font, they go back to their image tag
            boolean customFont = style.getFont() instanceof FontDescription.Resource resource
                    && !resource.equals(FontDescription.DEFAULT);
            List<String> images = new ArrayList<>();
            boolean allImages = customFont;
            for (int codepoint : content.codePoints().toArray()) {
                String id = customFont ? CraftEngineImages.get().idOf(style.getFont(), new String(Character.toChars(codepoint))) : null;
                images.add(id);
                if(id == null) allImages = false;
            }

            //A font that only carries images needs no tag of its own
            Style effective = allImages ? style.withFont(FontDescription.DEFAULT) : style;
            if(!effective.equals(previous[0])){
                if(!previous[0].isEmpty()) out.append("<reset>");
                appendStyle(out, effective);
                previous[0] = effective;
            }

            int index = 0;
            for (int codepoint : content.codePoints().toArray()) {
                String id = images.get(index++);
                if(id != null) out.append("<image:").append(id).append('>');
                //Icons are private use characters: invisible in a text field, and lost on the way to an AI
                else if(Character.getType(codepoint) == Character.PRIVATE_USE){
                    out.append("<glyph:").append(Integer.toHexString(codepoint).toUpperCase(Locale.ROOT)).append('>');
                }
                else out.append(new String(Character.toChars(codepoint)).replace("\\", "\\\\").replace("<", "\\<"));
            }
            return Optional.empty();
        }, Style.EMPTY);

        return out.toString();
    }

    private static void appendStyle(StringBuilder out, Style style){
        if(style.getColor() != null) out.append('<').append(style.getColor().serialize().toLowerCase(Locale.ROOT)).append('>');
        if(style.getFont() instanceof FontDescription.Resource resource && !resource.equals(FontDescription.DEFAULT)){
            out.append("<font:").append(resource.id()).append('>');
        }
        decoration(out, "bold", style.isBold(), !style.equals(style.withBold(null)));
        decoration(out, "italic", style.isItalic(), !style.equals(style.withItalic(null)));
        decoration(out, "underlined", style.isUnderlined(), !style.equals(style.withUnderlined(null)));
        decoration(out, "strikethrough", style.isStrikethrough(), !style.equals(style.withStrikethrough(null)));
        decoration(out, "obfuscated", style.isObfuscated(), !style.equals(style.withObfuscated(null)));
    }

    private static void decoration(StringBuilder out, String name, boolean on, boolean explicit){
        if(on) out.append('<').append(name).append('>');
        //The getters say false for off and for not set, only an explicit off has to be written down
        else if(explicit) out.append("<!").append(name).append('>');
    }

}
