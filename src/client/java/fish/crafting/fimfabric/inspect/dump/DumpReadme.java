package fish.crafting.fimfabric.inspect.dump;

import fish.crafting.fimfabric.inspect.pack.PackReport;
import fish.crafting.fimfabric.inspect.pack.PackReport.*;
import fish.crafting.fimfabric.inspect.view.ViewSnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The first file an LLM opens: what the folder is, how to read it and where to start looking.
 */
public class DumpReadme {

    public static String build(@Nullable ViewSnapshot snapshot, PackReport report){
        StringBuilder out = new StringBuilder();

        out.append("# FIM dump\n\n");
        if(snapshot != null){
            out.append("What a Minecraft client was showing at one moment on `").append(snapshot.label())
                    .append("`, together with the fonts and shaders of its resource packs. ")
                    .append("The goal is to rebuild the same look on another server.\n\n");
        }else{
            out.append("The fonts and shaders of the resource packs a Minecraft client had active.\n\n");
        }

        out.append("## Files\n\n");
        if(snapshot != null){
            out.append("- `view.json` - the open screen (dialog or container), boss bars, title, action bar, scoreboard and tab list\n");
            out.append("- `screenshot.png` - the frame `view.json` describes\n");
        }
        out.append("- `packs.json` - the active packs with their size, every font they define and every shader they ship, ")
                .append("each with the path it was copied to\n");
        out.append("- `packs/<n>-<pack>/assets/...` - font definitions, the textures and font files they refer to, ")
                .append("and shader files, at the path they have inside the pack. Packs are numbered in load order, a later one wins.\n");
        out.append("- `vanilla/assets/...` - the vanilla version of every shader file a pack replaces. ")
                .append("Diff a pack's shader against it to see what was changed.\n\n");

        if(snapshot != null) viewGuide(out);
        if(snapshot != null) fontsUsed(out, snapshot, report);
        shaders(out, report);
        packs(out, report);

        return out.toString();
    }

    private static void viewGuide(StringBuilder out){
        out.append("## Reading view.json\n\n");
        out.append("Every text is an object with four fields:\n\n");
        out.append("- `component` - the text component as the server sent it. Send this to get the same text.\n");
        out.append("- `plain` - the text without styling\n");
        out.append("- `width` - its width in GUI units\n");
        out.append("- `runs` - the same text split wherever the style changes. Each run has its `font`, `color` and ")
                .append("`shadowColor` if set, and two arrays of equal length: `codepoints` and the `advances` ")
                .append("the cursor moves after each of them.\n\n");
        out.append("Things to look for:\n\n");
        out.append("- A negative advance moves the cursor left. Packs use such characters to place text and images ")
                .append("on top of each other or to push them off their usual position.\n");
        out.append("- A character from a private use block (U+E000 to U+F8FF) is usually an image: look its font up in ")
                .append("`packs.json`, the bitmap provider that lists the codepoint names the texture.\n");
        out.append("- A color is not always meant to be seen. The text shaders (`shaders/core/text*`, named `rendertype_text*` in older versions) often test the vertex color ")
                .append("to move, recolor or hide a piece of text, so an odd color like `#FEFEFE` or `#4E5C24` is likely a marker. ")
                .append("Search the shaders for that value, as 0-255 integers or as fractions of 255.\n");
        out.append("- All coordinates are GUI units; one of them is `meta.window.guiScale` pixels in the screenshot.\n");
        out.append("- A dialog is under `screen.dialog` in the format of a dialog file, and can be used as one.\n");
        out.append("- An item under `screen.container.slots[].item.stack` is in the format `/give` and loot tables use.\n\n");
    }

    private static void fontsUsed(StringBuilder out, ViewSnapshot snapshot, PackReport report){
        out.append("## Fonts used by this view\n\n");
        if(snapshot.fontsUsed().isEmpty()){
            out.append("No text is on screen.\n\n");
            return;
        }

        Set<String> defined = report.fonts().stream().map(font -> font.id().toString()).collect(Collectors.toSet());

        out.append("| Font | Defined by a pack | Used at |\n|---|---|---|\n");
        snapshot.fontsUsed().forEach((font, places) -> {
            String where = places.stream().limit(6).map(place -> "`" + place + "`").collect(Collectors.joining(", "));
            if(places.size() > 6) where += " and " + (places.size() - 6) + " more";

            out.append("| `").append(font).append("` | ").append(defined.contains(font) ? "yes" : "no")
                    .append(" | ").append(where).append(" |\n");
        });
        out.append('\n');
    }

    private static void shaders(StringBuilder out, PackReport report){
        out.append("## Shaders shipped by packs\n\n");
        if(report.shaders().isEmpty()){
            out.append("None. Whatever this server does, it does with fonts and textures alone.\n\n");
            return;
        }

        out.append("| File | Kind | Pack | Replaces vanilla | In use |\n|---|---|---|---|---|\n");
        for (ShaderInfo shader : report.shaders()) {
            out.append("| `").append(shader.location()).append("` | ").append(shader.kind())
                    .append(" | ").append(shader.file().packId())
                    .append(" | ").append(shader.vanilla() != null ? "yes" : "no, new file")
                    .append(" | ").append(shader.active() ? "yes" : "no, another pack overrides it")
                    .append(" |\n");
        }
        out.append('\n');
    }

    private static void packs(StringBuilder out, PackReport report){
        out.append("## Packs\n\n");
        if(report.packs().isEmpty()){
            out.append("No resource pack besides vanilla is active.\n");
            return;
        }

        out.append("| Pack | Source | Size (uncompressed) | Files | Fonts |\n|---|---|---|---|---|\n");
        for (PackInfo pack : report.packs()) {
            long fonts = report.fonts().stream()
                    .filter(font -> font.definitions().stream().anyMatch(definition -> definition.file().packId().equals(pack.id())))
                    .count();

            out.append("| ").append(pack.title()).append(" (`").append(pack.id()).append("`) | ").append(pack.source())
                    .append(" | ").append(size(pack.bytes()))
                    .append(" | ").append(pack.files())
                    .append(" | ").append(fonts)
                    .append(" |\n");
        }
    }

    public static String size(long bytes){
        if(bytes < 1024) return bytes + " B";
        if(bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

}
