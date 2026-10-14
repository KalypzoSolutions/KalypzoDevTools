package fish.crafting.fimfabric.inspect.item;

import com.mojang.serialization.JsonOps;
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
    private static final int PREVIEW_WIDTH = 240;

    //Quick buttons: label, text that goes into the field
    private static final String[][] TAGS = {
            {"B", "<bold>"}, {"I", "<italic>"}, {"U", "<underlined>"}, {"S", "<strikethrough>"}, {"?", "<obfuscated>"},
            {"!I", "<!italic>"}, {"Reset", "<reset>"},
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
    private FlowLayout preview;
    private LabelComponent warnings;
    private io.wispforest.owo.ui.core.UIComponent lastFocused;

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

        FlowLayout editor = UIContainers.verticalFlow(Sizing.fill(58), Sizing.fill(100));
        editor.gap(4);
        body.child(editor);

        editor.child(UIComponents.label(Component.literal("Name (MiniMessage)")).color(MUTED));
        nameBox = UIComponents.textBox(Sizing.fill(100), MiniMessage.serialize(
                target.has(DataComponents.CUSTOM_NAME) ? target.get(DataComponents.CUSTOM_NAME) : Component.empty()));
        nameBox.setMaxLength(2048);
        nameBox.onChanged().subscribe(text -> refresh());
        nameBox.focusGained().subscribe(source -> lastFocused = nameBox);
        editor.child(nameBox);

        editor.child(UIComponents.label(Component.literal("Lore (MiniMessage, one line per row)")).color(MUTED));
        loreArea = UIComponents.textArea(Sizing.fill(100), Sizing.expand(), loreText());
        loreArea.onChanged().subscribe(text -> refresh());
        loreArea.focusGained().subscribe(source -> lastFocused = loreArea);
        lastFocused = loreArea;
        editor.child(loreArea);

        editor.child(tagButtons());

        warnings = UIComponents.label(Component.empty());
        warnings.color(WARNING);
        warnings.horizontalSizing(Sizing.fill(100));
        editor.child(warnings);

        editor.child(actionButtons());

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

        refresh();
    }

    private String loreText(){
        ItemLore lore = target.get(DataComponents.LORE);
        if(lore == null) return "";

        List<String> lines = new ArrayList<>();
        for (Component line : lore.lines()) lines.add(MiniMessage.serialize(line));
        return String.join("\n", lines);
    }

    private FlowLayout tagButtons(){
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(2);
        //Wraps like text, the editor is not wide enough for all of them in one row
        FlowLayout wrapped = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        wrapped.gap(2);

        FlowLayout current = row;
        int width = 0;
        for (String[] tag : TAGS) {
            int buttonWidth = this.font.width(tag[0]) + 10;
            if(width + buttonWidth > Math.max(150, (int) (this.width * 0.58) - 30)){
                wrapped.child(current);
                current = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
                current.gap(2);
                width = 0;
            }

            ButtonComponent button = UIComponents.button(Component.literal(tag[0]), b -> insert(tag[1]));
            button.horizontalSizing(Sizing.fixed(buttonWidth));
            button.verticalSizing(Sizing.fixed(16));
            button.tooltip(Component.literal(tag[1]));
            current.child(button);
            width += buttonWidth + 2;
        }
        wrapped.child(current);
        return wrapped;
    }

    private FlowLayout actionButtons(){
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.gap(4);

        row.child(UIComponents.button(Component.literal("Apply"), button -> apply(false)));
        row.child(UIComponents.button(Component.literal("Apply & Close"), button -> apply(true)));
        row.child(UIComponents.button(Component.literal("Reset"), button -> reset()));
        row.child(UIComponents.button(Component.literal("Copy MiniMessage"), button -> copyMiniMessage()));
        row.child(UIComponents.button(Component.literal("Copy /give"), button -> copyGive()));
        return row;
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
            LabelComponent label = UIComponents.label(line);
            label.shadow(true);
            label.horizontalSizing(Sizing.fixed(PREVIEW_WIDTH));
            preview.child(label);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

}
