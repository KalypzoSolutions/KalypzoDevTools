package fish.crafting.fimfabric.inspect.screen;

import fish.crafting.fimfabric.client.FIMModClient;
import fish.crafting.fimfabric.inspect.dump.DumpReadme;
import fish.crafting.fimfabric.inspect.dump.Dumper;
import fish.crafting.fimfabric.inspect.pack.PackInspector;
import fish.crafting.fimfabric.inspect.pack.PackReport;
import fish.crafting.fimfabric.inspect.pack.PackReport.*;
import fish.crafting.fimfabric.inspect.view.TextEncoder;
import fish.crafting.fimfabric.inspect.view.ViewCollector;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Shows what the active resource packs bring along: their size, their fonts with a preview and the
 * shaders they replace.
 */
public class PackAnalysisScreen extends BaseOwoScreen<FlowLayout> {

    private static final Color MUTED = Color.ofRgb(0xAAAAAA);
    private static final Color FAILED = Color.ofRgb(0xFF5555);
    private static final int MAX_PREVIEW_GLYPHS = 96;
    private static final int MAX_LISTED_ADVANCES = 8;
    //Packs hide glyphs by moving them thousands of pixels off screen, the preview should not follow them there
    private static final int MAX_PREVIEW_EXTENT = 300;
    //Text sits on a baseline this far below the top of its line
    private static final int BASELINE = 7;

    private final @Nullable Screen parent;
    private FlowLayout content;
    private ButtonComponent dumpButton;

    private PackAnalysisScreen(@Nullable Screen parent) {
        super(Component.literal("Resource Pack Analysis"));
        this.parent = parent;
    }

    /**
     * The only way in from code that has to load without owo: a call does not pull this class in
     * before it runs, creating the screen over there would.
     */
    public static void open(){
        Minecraft mc = Minecraft.getInstance();
        //Swapped right away, the click that led here would go on into the new screen
        mc.schedule(() -> mc.gui.setScreen(new PackAnalysisScreen(mc.gui.screen())));
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.gap(6);
        root.padding(Insets.of(10));
        root.surface(Surface.VANILLA_TRANSLUCENT);

        dumpButton = UIComponents.button(Component.literal("Dump Sources"), button -> {});
        dumpButton.active(false);
        dumpButton.tooltip(Component.literal("Writes every font and shader to a folder, with the vanilla shaders next to them"));

        FlowLayout header = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.gap(6);
        header.verticalAlignment(VerticalAlignment.CENTER);
        header.child(UIComponents.label(getTitle()).shadow(true));
        //A spacer grows in both directions, and a header as tall as the screen leaves no room for the content
        header.child(UIComponents.spacer().verticalSizing(Sizing.fixed(1)));
        header.child(dumpButton);
        header.child(UIComponents.button(Component.literal("Done"), button -> onClose()));
        root.child(header);

        content = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        content.gap(4);
        content.padding(Insets.right(8));
        content.child(UIComponents.label(Component.literal("Reading the packs...")).color(MUTED));
        root.child(UIContainers.verticalScroll(Sizing.fill(100), Sizing.expand(), content));

        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> PackInspector.inspect(mc.getResourceManager()), Util.backgroundExecutor())
                .whenComplete((report, error) -> mc.execute(() -> {
                    if(error != null) fail(error);
                    else show(report);
                }));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private void fail(Throwable error){
        FIMModClient.LOGGER.error("Could not analyze the resource packs", error);
        content.clearChildren();
        content.child(UIComponents.label(Component.literal("Could not read the packs: " + error)).color(FAILED));
    }

    private void show(PackReport report){
        content.clearChildren();

        dumpButton.onPress(button -> Dumper.dumpPacks(report, ViewCollector.label(Minecraft.getInstance())));
        dumpButton.active(!report.fonts().isEmpty() || !report.shaders().isEmpty());

        heading("Packs");
        if(report.packs().isEmpty()) muted("No resource pack besides vanilla is active.");
        for (PackInfo pack : report.packs()) {
            line(pack.title() + "  -  " + DumpReadme.size(pack.bytes()) + " uncompressed, " + pack.files() + " files  (" + pack.source() + ")");

            String categories = pack.bytesByCategory().entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .map(entry -> entry.getKey() + " " + DumpReadme.size(entry.getValue()))
                    .collect(Collectors.joining("  |  "));
            if(!categories.isEmpty()) muted(categories);
        }

        heading("Fonts (" + report.fonts().size() + ")");
        if(report.fonts().isEmpty()) muted("No pack defines a font.");
        for (FontInfo font : report.fonts()) {
            content.child(font(font));
        }

        heading("Shaders (" + report.shaders().size() + ")");
        if(report.shaders().isEmpty()) muted("No pack ships a shader.");
        report.shaders().stream()
                //Core shaders are what changes how text and the interface are drawn
                .sorted(Comparator.comparing((ShaderInfo shader) -> !shader.kind().equals("core")))
                .forEach(shader -> {
                    line(shader.location().toString());
                    muted(shader.kind()
                            + (shader.vanilla() != null ? ", replaces vanilla" : ", new file")
                            + (shader.active() ? "" : ", overridden by another pack")
                            + "  |  " + shader.file().packId());
                });
    }

    private CollapsibleContainer font(FontInfo font){
        List<FontProvider> providers = font.definitions().stream()
                .flatMap(definition -> definition.providers().stream())
                .toList();

        String summary = providers.stream()
                .collect(Collectors.groupingBy(FontProvider::type, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> entry.getValue() + " " + entry.getKey())
                .collect(Collectors.joining(", "));

        CollapsibleContainer container = UIContainers.collapsible(Sizing.fill(100), Sizing.content(),
                Component.literal(font.id() + "  -  " + (summary.isEmpty() ? "no providers" : summary)), false);

        for (FontDefinition definition : font.definitions()) {
            if(definition.error() != null){
                container.child(label(definition.file().packId() + ": " + definition.error()).color(FAILED));
            }
        }

        preview(container, font, providers);

        for (FontProvider provider : providers) {
            container.child(label(describe(provider)).color(MUTED));
        }

        return container;
    }

    /**
     * The glyphs of the bitmap providers, drawn in the font itself.
     */
    private void preview(CollapsibleContainer container, FontInfo font, List<FontProvider> providers){
        Font renderer = Minecraft.getInstance().font;
        Style style = Style.EMPTY.withFont(new FontDescription.Resource(font.id()));
        StringBuilder glyphs = new StringBuilder();
        int count = 0;
        //Packs stretch a single pixel over the whole screen to fade it, drawn here it would bury the list
        int tooWide = 0;
        //How far a glyph reaches above and below the line it is drawn in
        int above = 0;
        int below = 0;

        for (FontProvider provider : providers) {
            if(!provider.type().equals("bitmap")) continue;

            above = Math.max(above, provider.ascent() - BASELINE);
            below = Math.max(below, provider.height() - provider.ascent() + BASELINE - renderer.lineHeight);

            for (int codepoint : provider.codepoints()) {
                //Empty cells of the grid
                if(codepoint == 0 || count >= MAX_PREVIEW_GLYPHS) continue;

                if(renderer.width(Component.literal(Character.toString(codepoint)).withStyle(style)) > labelWidth()){
                    tooWide++;
                    continue;
                }

                glyphs.appendCodePoint(codepoint).append(' ');
                count++;
            }
        }

        if(tooWide > 0){
            container.child(label(tooWide + (tooWide == 1 ? " glyph is" : " glyphs are") + " too wide to preview").color(MUTED));
        }

        if(count == 0) return;
        above = Mth.clamp(above, 0, MAX_PREVIEW_EXTENT);
        below = Mth.clamp(below, 0, MAX_PREVIEW_EXTENT);

        //The label draws text at the bottom of its line, so the room above goes into the line and the room below between lines
        LabelComponent label = label(Component.literal(glyphs.toString()).withStyle(style));
        label.lineHeight(renderer.lineHeight + above);
        label.lineSpacing(below);
        label.margins(Insets.of(2, below + 2, 0, 0));
        container.child(label);
    }

    private static String describe(FontProvider provider){
        List<String> parts = new ArrayList<>();
        parts.add(provider.type());
        if(provider.reference() != null) parts.add(provider.reference().toString());

        if(provider.type().equals("bitmap")){
            long glyphs = Arrays.stream(provider.codepoints()).filter(codepoint -> codepoint != 0).count();
            parts.add("height " + provider.height());
            parts.add("ascent " + provider.ascent());
            parts.add(glyphs + (glyphs == 1 ? " glyph" : " glyphs"));
        }

        if(!provider.advances().isEmpty()){
            String advances = provider.advances().entrySet().stream()
                    .limit(MAX_LISTED_ADVANCES)
                    .map(entry -> TextEncoder.codepoint(entry.getKey()) + " " + entry.getValue())
                    .collect(Collectors.joining(", "));
            if(provider.advances().size() > MAX_LISTED_ADVANCES) advances += ", ...";
            parts.add(provider.advances().size() + " advances: " + advances);
        }

        return String.join("  |  ", parts);
    }

    private void heading(String text){
        content.child(UIComponents.label(Component.literal(text)).shadow(true).margins(Insets.top(8)));
    }

    private void line(String text){
        content.child(label(text));
    }

    private void muted(String text){
        content.child(label(text).color(MUTED));
    }

    private LabelComponent label(String text){
        return label(Component.literal(text));
    }

    /**
     * Wraps instead of running off the screen.
     */
    private LabelComponent label(Component text){
        LabelComponent label = UIComponents.label(text);
        //Not maxWidth: owo wraps again and again without end when a single glyph is wider than that
        label.horizontalSizing(Sizing.fixed(labelWidth()));
        return label;
    }

    private int labelWidth(){
        return Math.max(100, this.width - 60);
    }

}
