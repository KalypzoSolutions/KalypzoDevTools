package fish.crafting.fimfabric.inspect.item;

import com.mojang.serialization.JsonOps;
import fish.crafting.fimfabric.ai.ItemTextPrompt;
import fish.crafting.fimfabric.minimessage.CraftEngineImages;
import fish.crafting.fimfabric.minimessage.MiniMessage;
import fish.crafting.fimfabric.minimessage.MiniMessageTextArea;
import fish.crafting.fimfabric.minimessage.MiniMessageToolbar;
import fish.crafting.fimfabric.ai.LocalAi;
import fish.crafting.fimfabric.rendering.InformationFeedManager;
import fish.crafting.fimfabric.util.SoundUtil;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.OverlayContainer;
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

    //What the server sent for a stack before the first client side edit, so it can be brought back
    private static final java.util.Map<ItemStack, ItemStack> SERVER_STATE =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private final @Nullable Screen parent;
    //The stack that gets changed, and what it looked like before
    private final ItemStack target;
    private final ItemStack original;

    private MiniMessageTextArea nameBox;
    private MiniMessageTextArea loreArea;
    private FlowLayout rootLayout;
    private ButtonComponent modelButton;
    private ButtonComponent rewriteButton;
    private ButtonComponent undoButton;
    private FlowLayout preview;
    private LabelComponent warnings;
    private LabelComponent status;
    private MiniMessageTextArea lastFocused;

    //The AI panel only exists while it is open, the task text outlives it
    private @Nullable OverlayContainer<FlowLayout> aiOverlay;
    private @Nullable TextAreaComponent aiTaskArea;
    private @Nullable LabelComponent aiStatus;
    private @Nullable LabelComponent aiModelLabel;
    private String aiTask = "";

    private final List<LocalAi.Backend> backends = LocalAi.available();
    private @Nullable LocalAi.Backend selectedBackend = null;
    //Null stands for the default model of the tool
    private @Nullable String selectedModel = null;
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

        rootLayout = root;

        //Header
        FlowLayout header = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.gap(6);
        header.verticalAlignment(VerticalAlignment.CENTER);
        header.child(UIComponents.item(target.copy()));
        header.child(UIComponents.label(Component.literal("Item Editor  -  "
                + BuiltInRegistries.ITEM.getKey(target.getItem()))).shadow(true));
        header.child(UIComponents.spacer().verticalSizing(Sizing.fixed(1)));

        if(!backends.isEmpty() && selectedBackend == null) selectedBackend = backends.getFirst();

        //One button in two parts: the left opens the assistant, the right drops down tool and model
        FlowLayout split = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        split.gap(0);
        split.child(button("AI Assistant", this::toggleAiPanel));
        modelButton = button("v", this::openModelMenu);
        modelButton.horizontalSizing(Sizing.fixed(18));
        modelButton.tooltip(Component.literal(modelLabel()));
        split.child(modelButton);
        header.child(split);

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
        nameBox = new MiniMessageTextArea(Sizing.fill(100), Sizing.fixed(30), MiniMessage.serialize(
                target.has(DataComponents.CUSTOM_NAME) ? target.get(DataComponents.CUSTOM_NAME) : Component.empty()));
        nameBox.setCharacterLimit(2048);
        //A name is one line
        nameBox.maxLines(1);
        nameBox.onChanged().subscribe(text -> refresh());
        nameBox.focusGained().subscribe(source -> lastFocused = nameBox);
        editor.child(nameBox);

        editor.child(UIComponents.label(Component.literal("Lore  (one line per row)")).color(MUTED));
        loreArea = new MiniMessageTextArea(Sizing.fill(100), Sizing.fixed(Math.max(90, this.height / 3)), loreText());
        loreArea.setCharacterLimit(32767);
        loreArea.onChanged().subscribe(text -> refresh());
        loreArea.focusGained().subscribe(source -> lastFocused = loreArea);
        lastFocused = loreArea;
        editor.child(loreArea);

        warnings = UIComponents.label(Component.empty());
        warnings.color(WARNING);
        warnings.horizontalSizing(Sizing.fill(100));
        editor.child(warnings);

        status = UIComponents.label(Component.empty());
        status.color(MUTED);
        status.horizontalSizing(Sizing.fill(100));
        editor.child(status);

        //One toolbar for both fields, it acts on the one that was used last
        editor.child(section("Format  (select text first, Ctrl+Z undo, Ctrl+B/I/U)"));
        editor.child(MiniMessageToolbar.create(editorWidth(), () -> lastFocused, List.of(
                new MiniMessageToolbar.Extra("No italic", "<!italic>", null),
                new MiniMessageToolbar.Extra("Reset", "<reset>", null),
                new MiniMessageToolbar.Extra("Image", "<image:default:name>", null),
                new MiniMessageToolbar.Extra("Font", "<font:minecraft:default>", "</font>"),
                new MiniMessageToolbar.Extra("Shift", "<shift:8>", null)
        )));

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
        undoButton = button("Undo AI", this::undoAi);
        undoButton.active(beforeAi != null);

        FlowLayout actions = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        actions.surface(Surface.flat(0x66000000));
        actions.padding(Insets.of(4));
        actions.child(buttonRows(List.of(
                button("Apply", () -> apply(false)),
                button("Apply & Close", () -> apply(true)),
                button("Reset", this::reset),
                button("Restore server state", this::restoreServerState),
                undoButton,
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

    // ------------------------------------------------------------------ AI

    private String modelLabel(){
        if(selectedBackend == null) return "No AI found";
        return selectedBackend.label() + " / " + (selectedModel == null ? "default" : selectedModel);
    }

    private void select(LocalAi.Backend backend, @Nullable String model){
        selectedBackend = backend;
        selectedModel = model;
        modelButton.tooltip(Component.literal(modelLabel()));
        if(aiModelLabel != null) aiModelLabel.text(Component.literal("Using: " + modelLabel()));
        if(rewriteButton != null) rewriteButton.active(!asking);
    }

    /**
     * The second button of the AI pair: picks the tool and, below it, the model.
     */
    private void openModelMenu(){
        //One flat list: submenus would open to the right of a button that already sits at the screen edge
        int menuWidth = 190;
        int x = Math.max(4, Math.min(modelButton.x() + modelButton.width() - menuWidth, this.width - menuWidth - 4));

        DropdownComponent.openContextMenu(this, rootLayout, FlowLayout::child,
                x, modelButton.y() + modelButton.height(), menu -> {
                    menu.text(Component.literal("Using: " + modelLabel()));
                    menu.divider();
                    if(backends.isEmpty()){
                        menu.text(Component.literal("Neither claude nor codex was found"));
                        menu.text(Component.literal("on the PATH of this game"));
                        return;
                    }

                    for (LocalAi.Backend backend : backends) {
                        menu.text(Component.literal(backend.label()));
                        menu.button(Component.literal("   default"), d -> select(backend, null));
                        for (String model : backend.models()) {
                            menu.button(Component.literal("   " + model), d -> select(backend, model));
                        }
                    }
                });
    }

    /**
     * Everything known about the item, one fact per line, in the words of the inspector.
     */
    private String itemData(){
        StringBuilder data = new StringBuilder();
        for (Inspection.Row row : Inspection.ofItem(target).rows()) {
            if(row.heading()){
                data.append('\n').append(row.key()).append(":\n");
            } else if(row.key().isEmpty()){
                data.append("  ").append(row.value()).append('\n');
            } else {
                data.append("  ").append(row.key()).append(": ").append(row.value()).append('\n');
            }
        }
        return data.toString().strip();
    }

    private void toggleAiPanel(){
        if(aiOverlay != null){
            closeAiPanel();
            return;
        }

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fixed(Math.clamp(this.width - 40, 200, 440)), Sizing.content());
        panel.surface(Surface.DARK_PANEL);
        panel.padding(Insets.of(10));
        panel.gap(6);
        panel.positioning(Positioning.relative(50, 40));

        panel.child(UIComponents.label(Component.literal("AI Assistant")).color(SECTION).shadow(true));

        LabelComponent sent = UIComponents.label(Component.literal(
                "Sent to the AI: item id, count, all components, tags, the current name and lore, and your task. "
                        + "The AI answers with a new name and lore that replace the fields."));
        sent.color(MUTED);
        sent.horizontalSizing(Sizing.fill(100));
        panel.child(sent);

        panel.child(UIComponents.label(Component.literal("Task")).color(MUTED));
        aiTaskArea = UIComponents.textArea(Sizing.fill(100), Sizing.fixed(90), aiTask);
        aiTaskArea.onChanged().subscribe(text -> aiTask = text);
        panel.child(aiTaskArea);

        LabelComponent hint = UIComponents.label(Component.literal(
                "e.g. \"make it sound legendary\", \"gold gradient name, three short lore lines\", \"translate to German\""));
        hint.color(MUTED);
        hint.horizontalSizing(Sizing.fill(100));
        panel.child(hint);

        aiModelLabel = UIComponents.label(Component.literal("Using: " + modelLabel()));
        aiModelLabel.color(MUTED);
        panel.child(aiModelLabel);

        aiStatus = UIComponents.label(Component.empty());
        aiStatus.color(WARNING);
        aiStatus.horizontalSizing(Sizing.fill(100));
        panel.child(aiStatus);

        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(4);
        rewriteButton = button("Run", this::rewrite);
        rewriteButton.active(selectedBackend != null && !asking);
        row.child(rewriteButton);
        row.child(button("Close", this::closeAiPanel));
        panel.child(row);

        aiOverlay = UIContainers.overlay(panel);
        aiOverlay.closeOnClick(false);
        rootLayout.child(aiOverlay);
    }

    private void closeAiPanel(){
        if(aiOverlay == null) return;

        aiOverlay.remove();
        aiOverlay = null;
        aiTaskArea = null;
        aiStatus = null;
        aiModelLabel = null;
        rewriteButton = null;
    }

    private void aiMessage(String text, boolean problem){
        if(aiStatus == null) return;
        aiStatus.color(problem ? WARNING : MUTED);
        aiStatus.text(Component.literal(text));
    }

    private void rewrite(){
        if(asking || selectedBackend == null) return;

        String prompt = ItemTextPrompt.build(itemData(), nameBox.getValue(), loreArea.getValue(), aiTask,
                CraftEngineImages.get().ids());
        LocalAi.Backend backend = selectedBackend;

        asking = true;
        rewriteButton.active(false);
        aiMessage("Asking " + modelLabel() + "... this can take a minute", false);

        Minecraft mc = Minecraft.getInstance();
        LocalAi.ask(backend, selectedModel, prompt).whenComplete((reply, error) -> mc.execute(() -> {
            asking = false;
            if(closed) return;
            if(rewriteButton != null) rewriteButton.active(true);

            if(error != null){
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                aiMessage(String.valueOf(cause.getMessage()), true);
                return;
            }

            ItemTextPrompt.Answer answer;
            try {
                answer = ItemTextPrompt.parse(reply);
            } catch (IllegalArgumentException e) {
                aiMessage(e.getMessage(), true);
                return;
            }

            if(!answer.changesAnything()){
                aiMessage(answer.notes() != null ? answer.notes() : "The AI changed nothing", false);
                return;
            }

            beforeAi = new String[]{nameBox.getValue(), loreArea.getValue()};
            undoButton.active(true);
            if(answer.name() != null) nameBox.replaceAll(answer.name());
            if(answer.lore() != null) loreArea.replaceAll(String.join("\n", answer.lore()));
            refresh();

            closeAiPanel();
            status.text(Component.literal("AI: " + (answer.notes() != null ? answer.notes() : "applied to the fields")
                    + "  (Undo AI brings your text back)"));
        }));
    }

    private void undoAi(){
        if(beforeAi == null) return;

        nameBox.replaceAll(beforeAi[0]);
        loreArea.replaceAll(beforeAi[1]);
        beforeAi = null;
        undoButton.active(false);
        status.text(Component.empty());
        refresh();
    }

    // ------------------------------------------------------------------ actions

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

    /**
     * Brings the item back the way the server sent it, however often it was edited here.
     */
    private void restoreServerState(){
        ItemStack server = SERVER_STATE.get(target);
        if(server == null){
            InformationFeedManager.info("Nothing to restore, the item is as the server sent it", false);
            return;
        }

        Component name = server.get(DataComponents.CUSTOM_NAME);
        if(name == null) target.remove(DataComponents.CUSTOM_NAME);
        else target.set(DataComponents.CUSTOM_NAME, name);

        ItemLore lore = server.get(DataComponents.LORE);
        if(lore == null) target.remove(DataComponents.LORE);
        else target.set(DataComponents.LORE, lore);
        SERVER_STATE.remove(target);

        //The fields follow, as one step that can be undone
        nameBox.replaceAll(name == null ? "" : MiniMessage.serialize(name));
        List<String> lines = new ArrayList<>();
        if(lore != null) for (Component line : lore.lines()) lines.add(MiniMessage.serialize(line));
        loreArea.replaceAll(String.join("\n", lines));

        InformationFeedManager.success("Server state restored", false);
        SoundUtil.clickSound();
        refresh();
    }

    private void apply(boolean close){
        //The first edit keeps what the server had
        SERVER_STATE.computeIfAbsent(target, ItemStack::copy);
        write(target);
        InformationFeedManager.success("Applied (client side only)", false);
        SoundUtil.clickSound();
        if(close) onClose();
    }

    private void reset(){
        Component name = original.get(DataComponents.CUSTOM_NAME);
        nameBox.replaceAll(name == null ? "" : MiniMessage.serialize(name));

        ItemLore lore = original.get(DataComponents.LORE);
        List<String> lines = new ArrayList<>();
        if(lore != null) for (Component line : lore.lines()) lines.add(MiniMessage.serialize(line));
        loreArea.replaceAll(String.join("\n", lines));
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
