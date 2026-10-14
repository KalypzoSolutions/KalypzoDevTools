package fish.crafting.fimfabric.inspect.item;

import com.mojang.serialization.JsonOps;
import fish.crafting.fimfabric.ai.ItemTextPrompt;
import fish.crafting.fimfabric.ai.LocalAi;
import fish.crafting.fimfabric.rendering.InformationFeedManager;
import fish.crafting.fimfabric.util.SoundUtil;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Edits the name and the lore of an item with MiniMessage, with a live preview of the tooltip.
 * Nothing leaves the client: the stack is only changed locally, the server overwrites it with its
 * own copy as soon as it sends the slot again.
 */
public class ItemEditorScreen extends BaseOwoScreen<FlowLayout> {

    private static final Color MUTED = Color.ofRgb(0xAAAAAA);
    private static final Color WARNING = Color.ofRgb(0xFFAA00);
    private static final Color SECTION = Color.ofRgb(0xFFD966);
    private static final int PREVIEW_WIDTH = 240;
    //Share of the screen the editor column gets, the preview takes the rest
    private static final double EDITOR_SHARE = 0.6;

    //Quick buttons: label, text that goes into the field
    private static final String[][] STYLE_TAGS = {
            {"Bold", "<bold>"}, {"Italic", "<italic>"}, {"Underline", "<underlined>"},
            {"Strike", "<strikethrough>"}, {"Obfuscated", "<obfuscated>"}, {"No italic", "<!italic>"}, {"Reset", "<reset>"}
    };
    private static final String[][] COLOR_TAGS = {
            {"Red", "<red>"}, {"Gold", "<gold>"}, {"Yellow", "<yellow>"}, {"Green", "<green>"},
            {"Aqua", "<aqua>"}, {"Blue", "<blue>"}, {"Purple", "<light_purple>"}, {"Gray", "<gray>"},
            {"Hex", "<#ff8800>"}, {"Gradient", "<gradient:#ff5555:#5555ff>"}, {"Rainbow", "<rainbow>"}
    };

    private final @Nullable Screen parent;
    //The stack that gets changed, and what it looked like before
    private final ItemStack target;
    private final ItemStack original;

    private TextBoxComponent nameBox;
    private TextAreaComponent loreArea;
    private TextBoxComponent instructionBox;
    private ButtonComponent backendButton;
    private ButtonComponent rewriteButton;
    private ButtonComponent undoButton;
    private FlowLayout preview;
    private LabelComponent warnings;
    private LabelComponent status;
    private io.wispforest.owo.ui.core.UIComponent lastFocused;

    private final List<LocalAi.Backend> backends = LocalAi.available();
    private int backend = 0;
    private boolean asking = false;
    private boolean closed = false;
    //What the fields held before the last AI answer replaced them
    private @Nullable String[] beforeAi = null;

    private ItemEditorScreen(@Nullable Screen parent, ItemStack target) {
        super(Component.literal("Item Editor"));
        this.parent = parent;
        this.target = target;
        this.original = target.copy();
    }

    /**
     * Like the pack analysis: the only entry point, so this class is not loaded without owo.
     */
    public static void open(@NotNull ItemStack stack){
        Minecraft mc = Minecraft.getInstance();
        Screen current = mc.gui.screen();
        mc.schedule(() -> mc.gui.setScreen(new ItemEditorScreen(current, stack)));
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

        //Header
        FlowLayout header = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.gap(6);
        header.verticalAlignment(VerticalAlignment.CENTER);
        header.child(UIComponents.item(target.copy()));
        header.child(UIComponents.label(Component.literal("Item Editor  -  "
                + BuiltInRegistries.ITEM.getKey(target.getItem()))).shadow(true));
        header.child(UIComponents.spacer().verticalSizing(Sizing.fixed(1)));
        header.child(UIComponents.button(Component.literal("Done"), button -> onClose()));
        root.child(header);

        //Body: editor on the left, preview on the right
        FlowLayout body = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.expand());
        body.gap(10);
        root.child(body);

        //The editor scrolls, the action bar below does not: the buttons stay reachable on any window size
        FlowLayout editor = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        editor.gap(4);
        editor.padding(Insets.right(8));
        body.child(UIContainers.verticalScroll(Sizing.fill((int) (EDITOR_SHARE * 100)), Sizing.fill(100), editor));

        editor.child(UIComponents.label(Component.literal("Name")).color(MUTED));
        nameBox = UIComponents.textBox(Sizing.fill(100), MiniMessage.serialize(
                target.has(DataComponents.CUSTOM_NAME) ? target.get(DataComponents.CUSTOM_NAME) : Component.empty()));
        nameBox.setMaxLength(2048);
        nameBox.onChanged().subscribe(text -> refresh());
        nameBox.focusGained().subscribe(source -> lastFocused = nameBox);
        editor.child(nameBox);

        editor.child(UIComponents.label(Component.literal("Lore  (one line per row)")).color(MUTED));
        loreArea = UIComponents.textArea(Sizing.fill(100), Sizing.fixed(Math.max(70, this.height / 4)), loreText());
        loreArea.onChanged().subscribe(text -> refresh());
        loreArea.focusGained().subscribe(source -> lastFocused = loreArea);
        lastFocused = loreArea;
        editor.child(loreArea);

        warnings = UIComponents.label(Component.empty());
        warnings.color(WARNING);
        warnings.horizontalSizing(Sizing.fill(100));
        editor.child(warnings);

        //Right below the text it works on, so it is the first thing seen
        editor.child(section("AI assistant"));
        editor.child(aiControls());

        editor.child(section("Styles"));
        editor.child(buttonRows(tagButtons(STYLE_TAGS), editorWidth()));
        editor.child(section("Colors"));
        editor.child(buttonRows(tagButtons(COLOR_TAGS), editorWidth()));

        //Preview
        FlowLayout side = UIContainers.verticalFlow(Sizing.expand(), Sizing.fill(100));
        side.gap(4);
        body.child(side);

        side.child(UIComponents.label(Component.literal("Preview")).color(MUTED));
        preview = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        preview.padding(Insets.of(6));
        preview.surface(Surface.flat(0xF0100010).and(Surface.outline(0xFF2D0A63)));
        preview.gap(1);
        side.child(UIContainers.verticalScroll(Sizing.fill(100), Sizing.expand(), preview));

        //Action bar
        FlowLayout actions = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        actions.surface(Surface.flat(0x66000000));
        actions.padding(Insets.of(4));
        actions.child(buttonRows(List.of(
                button("Apply", () -> apply(false)),
                button("Apply & Close", () -> apply(true)),
                button("Reset", this::reset),
                button("Copy MiniMessage", this::copyMiniMessage),
                button("Copy /give", this::copyGive),
                button("Copy as Java", this::copyJava)
        ), this.width - 40));
        root.child(actions);

        refresh();
    }

    private int editorWidth(){
        return (int) (this.width * EDITOR_SHARE) - 40;
    }

    private String loreText(){
        ItemLore lore = target.get(DataComponents.LORE);
        if(lore == null) return "";

        List<String> lines = new ArrayList<>();
        for (Component line : lore.lines()) lines.add(MiniMessage.serialize(line));
        return String.join("\n", lines);
    }

    // ------------------------------------------------------------------ layout helpers

    /**
     * A heading with a line under it, so the groups of buttons do not run into each other.
     */
    private FlowLayout section(String title){
        FlowLayout section = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        section.gap(2);
        section.margins(Insets.top(4));
        section.child(UIComponents.label(Component.literal(title)).color(SECTION));

        FlowLayout line = UIContainers.verticalFlow(Sizing.fill(100), Sizing.fixed(1));
        line.surface(Surface.flat(0x66FFFFFF));
        section.child(line);
        return section;
    }

    private ButtonComponent button(String label, Runnable action){
        ButtonComponent button = UIComponents.button(Component.literal(label), b -> action.run());
        button.horizontalSizing(Sizing.fixed(this.font.width(label) + 14));
        button.verticalSizing(Sizing.fixed(18));
        return button;
    }

    private List<ButtonComponent> tagButtons(String[][] tags){
        List<ButtonComponent> buttons = new ArrayList<>();
        for (String[] tag : tags) {
            ButtonComponent button = button(tag[0], () -> insert(tag[1]));
            button.tooltip(Component.literal(tag[1]));
            buttons.add(button);
        }
        return buttons;
    }

    /**
     * Puts the buttons into rows that break before they run out of the editor column.
     */
    private FlowLayout buttonRows(List<ButtonComponent> buttons, int available){
        FlowLayout rows = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        rows.gap(2);

        FlowLayout row = null;
        int used = 0;

        for (ButtonComponent button : buttons) {
            int width = button.horizontalSizing().get().value;
            if(row == null || used + width > available){
                row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
                row.gap(2);
                rows.child(row);
                used = 0;
            }

            row.child(button);
            used += width + 2;
        }
        return rows;
    }

    private FlowLayout aiControls(){
        FlowLayout box = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        box.gap(3);

        if(backends.isEmpty()){
            LabelComponent none = UIComponents.label(Component.literal("Neither claude (Claude Code) nor codex (OpenAI Codex) was found on the PATH of this game. Start the game from a terminal where the command works."));
            none.color(MUTED);
            none.horizontalSizing(Sizing.fill(100));
            box.child(none);
        }

        instructionBox = UIComponents.textBox(Sizing.fill(100));
        instructionBox.setMaxLength(500);
        instructionBox.setSuggestion("What should change? e.g. \"make it sound legendary\"");
        instructionBox.onChanged().subscribe(text -> instructionBox.setSuggestion(text.isEmpty()
                ? "What should change? e.g. \"make it sound legendary\"" : ""));
        box.child(instructionBox);

        backendButton = button(backendLabel(), this::cycleBackend);
        backendButton.horizontalSizing(Sizing.fixed(Math.max(120, this.font.width("AI: OpenAI Codex") + 14)));
        rewriteButton = button("Rewrite", this::rewrite);
        undoButton = button("Undo", this::undoAi);
        rewriteButton.active(!backends.isEmpty());
        undoButton.active(false);

        box.child(buttonRows(List.of(backendButton, rewriteButton, undoButton), editorWidth()));

        status = UIComponents.label(Component.empty());
        status.color(MUTED);
        status.horizontalSizing(Sizing.fill(100));
        box.child(status);
        return box;
    }

    // ------------------------------------------------------------------ AI

    private String backendLabel(){
        return backends.isEmpty() ? "AI: not found" : "AI: " + backends.get(backend).label();
    }

    private void cycleBackend(){
        if(backends.isEmpty()) return;
        backend = (backend + 1) % backends.size();
        backendButton.setMessage(Component.literal(backendLabel()));
    }

    private void rewrite(){
        if(asking || backends.isEmpty()) return;

        LocalAi.Backend chosen = backends.get(backend);
        String prompt = ItemTextPrompt.build(
                BuiltInRegistries.ITEM.getKey(target.getItem()).toString(),
                nameBox.getValue(), loreArea.getValue(), instructionBox.getValue());

        asking = true;
        rewriteButton.active(false);
        status.color(MUTED);
        status.text(Component.literal("Asking " + chosen.label() + "..."));

        Minecraft mc = Minecraft.getInstance();
        LocalAi.ask(chosen, prompt).whenComplete((reply, error) -> mc.execute(() -> {
            asking = false;
            if(closed) return;

            rewriteButton.active(true);
            if(error != null){
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                status.color(WARNING);
                status.text(Component.literal(String.valueOf(cause.getMessage())));
                return;
            }

            ItemTextPrompt.Answer answer = ItemTextPrompt.parse(reply);
            if(answer.isEmpty()){
                status.color(WARNING);
                status.text(Component.literal("The answer had no <name> or <lore> in it"));
                return;
            }

            beforeAi = new String[]{nameBox.getValue(), loreArea.getValue()};
            undoButton.active(true);
            if(answer.name() != null) nameBox.text(answer.name());
            if(answer.lore() != null) loreArea.text(answer.lore());

            status.color(MUTED);
            status.text(Component.literal("Done, Undo brings your text back"));
            refresh();
        }));
    }

    private void undoAi(){
        if(beforeAi == null) return;

        nameBox.text(beforeAi[0]);
        loreArea.text(beforeAi[1]);
        beforeAi = null;
        undoButton.active(false);
        refresh();
    }

    // ------------------------------------------------------------------ actions

    private void insert(String tag){
        if(lastFocused instanceof TextBoxComponent box) box.text(box.getValue() + tag);
        else if(lastFocused instanceof TextAreaComponent area) area.text(area.getValue() + tag);
    }

    /**
     * Writes the editor's content into a stack, without touching the client's own stack.
     */
    private void write(ItemStack stack){
        String name = nameBox.getValue();
        if(name.isBlank()) stack.remove(DataComponents.CUSTOM_NAME);
        else stack.set(DataComponents.CUSTOM_NAME, MiniMessage.parse(name).joined());

        String lore = loreArea.getValue();
        if(lore.isBlank()) stack.remove(DataComponents.LORE);
        else stack.set(DataComponents.LORE, new ItemLore(MiniMessage.parse(lore).lines()));
    }

    private void apply(boolean close){
        write(target);
        InformationFeedManager.success("Applied (client side only)", false);
        SoundUtil.clickSound();
        if(close) onClose();
    }

    private void reset(){
        Component name = original.get(DataComponents.CUSTOM_NAME);
        nameBox.text(name == null ? "" : MiniMessage.serialize(name));

        ItemLore lore = original.get(DataComponents.LORE);
        List<String> lines = new ArrayList<>();
        if(lore != null) for (Component line : lore.lines()) lines.add(MiniMessage.serialize(line));
        loreArea.text(String.join("\n", lines));
        refresh();
    }

    private void copyMiniMessage(){
        StringBuilder out = new StringBuilder();
        if(!nameBox.getValue().isBlank()) out.append("name: ").append(nameBox.getValue()).append('\n');
        if(!loreArea.getValue().isBlank()) out.append("lore:\n").append(loreArea.getValue());

        Minecraft.getInstance().keyboardHandler.setClipboard(out.toString());
        InformationFeedManager.success("Copied!", false);
        SoundUtil.clickSound();
    }

    private void copyGive(){
        ItemStack stack = target.copy();
        write(stack);

        var access = Minecraft.getInstance().level.registryAccess();
        var ops = access.createSerializationContext(JsonOps.INSTANCE);

        List<String> components = new ArrayList<>();
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if(name != null) {
            ComponentSerialization.CODEC.encodeStart(ops, name).result()
                    .ifPresent(json -> components.add("custom_name='" + escape(json.toString()) + "'"));
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if(lore != null){
            List<String> lines = new ArrayList<>();
            for (Component line : lore.lines()) {
                ComponentSerialization.CODEC.encodeStart(ops, line).result()
                        .ifPresent(json -> lines.add("'" + escape(json.toString()) + "'"));
            }
            components.add("lore=[" + String.join(",", lines) + "]");
        }

        String command = "/give @s " + BuiltInRegistries.ITEM.getKey(stack.getItem())
                + (components.isEmpty() ? "" : "[" + String.join(",", components) + "]");

        Minecraft.getInstance().keyboardHandler.setClipboard(command);
        InformationFeedManager.success("Copied!", false);
        SoundUtil.clickSound();
    }

    /**
     * Paper code that builds the item, with the texts kept as MiniMessage strings.
     */
    private void copyJava(){
        String material = BuiltInRegistries.ITEM.getKey(target.getItem()).getPath().toUpperCase(java.util.Locale.ROOT);
        String name = nameBox.getValue();
        List<String> lore = loreArea.getValue().isBlank() ? List.of() : List.of(loreArea.getValue().split("\n", -1));

        StringBuilder code = new StringBuilder();
        code.append("ItemStack item = new ItemStack(Material.").append(material);
        if(target.getCount() > 1) code.append(", ").append(target.getCount());
        code.append(");\n");

        if(!name.isBlank() || !lore.isEmpty()){
            code.append("item.editMeta(meta -> {\n");
            if(!name.isBlank()){
                code.append("    meta.displayName(MiniMessage.miniMessage().deserialize(\"")
                        .append(javaString(name)).append("\"));\n");
            }
            if(!lore.isEmpty()){
                code.append("    meta.lore(List.of(\n");
                for (int i = 0; i < lore.size(); i++) {
                    code.append("            MiniMessage.miniMessage().deserialize(\"").append(javaString(lore.get(i))).append("\")")
                            .append(i < lore.size() - 1 ? "," : "").append('\n');
                }
                code.append("    ));\n");
            }
            code.append("});\n");
        }

        Minecraft.getInstance().keyboardHandler.setClipboard(code.toString());
        InformationFeedManager.success("Copied!", false);
        SoundUtil.clickSound();
    }

    private static String javaString(String text){
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String escape(String json){
        return json.replace("\\", "\\\\").replace("'", "\\'");
    }

    // ------------------------------------------------------------------ preview

    private void refresh(){
        if(preview == null || nameBox == null || loreArea == null) return;

        ItemStack copy = target.copy();
        write(copy);

        List<String> problems = new ArrayList<>();
        problems.addAll(MiniMessage.parse(nameBox.getValue()).warnings());
        problems.addAll(MiniMessage.parse(loreArea.getValue()).warnings());
        warnings.text(Component.literal(problems.isEmpty() ? "" : String.join("  |  ", problems.stream().distinct().toList())));

        Minecraft mc = Minecraft.getInstance();
        List<Component> lines = copy.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL);

        preview.clearChildren();
        for (Component line : lines) {
            //An empty label has no height, blank lore lines would vanish from the preview
            if(line.getString().isEmpty()){
                preview.child(UIComponents.spacer().horizontalSizing(Sizing.fixed(1)).verticalSizing(Sizing.fixed(this.font.lineHeight)));
                continue;
            }

            LabelComponent label = UIComponents.label(line);
            label.shadow(true);
            label.horizontalSizing(Sizing.fixed(PREVIEW_WIDTH));
            preview.child(label);
        }
    }

    @Override
    public void onClose() {
        closed = true;
        Minecraft.getInstance().gui.setScreen(parent);
    }

}
