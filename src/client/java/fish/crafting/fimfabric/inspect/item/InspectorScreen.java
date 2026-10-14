package fish.crafting.fimfabric.inspect.item;

import fish.crafting.fimfabric.rendering.InformationFeedManager;
import fish.crafting.fimfabric.util.SoundUtil;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The inspector: IDs, components, block states and tags of an item or block, each with a copy button.
 */
public class InspectorScreen extends BaseOwoScreen<FlowLayout> {

    private static final Color MUTED = Color.ofRgb(0xAAAAAA);
    private static final Color HEADING = Color.ofRgb(0xFFD966);

    private final @Nullable Screen parent;
    private final Inspection inspection;

    private InspectorScreen(@Nullable Screen parent, Inspection inspection) {
        super(Component.literal("Inspector"));
        this.parent = parent;
        this.inspection = inspection;
    }

    /**
     * The only way in, so this class does not load without owo.
     */
    public static void open(@NotNull Inspection inspection){
        Minecraft mc = Minecraft.getInstance();
        Screen current = mc.gui.screen();
        mc.schedule(() -> mc.gui.setScreen(new InspectorScreen(current, inspection)));
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

        FlowLayout header = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        header.gap(6);
        header.verticalAlignment(VerticalAlignment.CENTER);
        header.child(UIComponents.item(inspection.icon()));
        header.child(UIComponents.label(Component.literal(inspection.title())).shadow(true));
        header.child(UIComponents.spacer().verticalSizing(Sizing.fixed(1)));

        if(inspection.editable() != null){
            header.child(UIComponents.button(Component.literal("Edit name & lore"), button -> {
                //The editor returns to the screen the inspector came from
                Minecraft.getInstance().gui.setScreen(parent);
                ItemEditorScreen.open(inspection.editable());
            }));
        }
        header.child(UIComponents.button(Component.literal("Done"), button -> onClose()));
        root.child(header);

        FlowLayout content = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        content.gap(3);
        content.padding(Insets.right(8));

        for (Inspection.Row row : inspection.rows()) {
            if(row.heading()){
                content.child(UIComponents.label(Component.literal(row.key())).color(HEADING).shadow(true).margins(Insets.top(8)));
                continue;
            }
            content.child(rowOf(row));
        }

        root.child(UIContainers.verticalScroll(Sizing.fill(100), Sizing.expand(), content));
    }

    private FlowLayout rowOf(Inspection.Row row){
        FlowLayout line = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        line.gap(6);
        line.verticalAlignment(VerticalAlignment.CENTER);

        if(row.copy() != null){
            var button = UIComponents.button(Component.literal("Copy"), b -> {
                Minecraft.getInstance().keyboardHandler.setClipboard(row.copy());
                InformationFeedManager.success("Copied!", false);
                SoundUtil.clickSound();
            });
            button.horizontalSizing(Sizing.fixed(36));
            button.verticalSizing(Sizing.fixed(14));
            line.child(button);
        } else {
            line.child(UIComponents.spacer().horizontalSizing(Sizing.fixed(36)).verticalSizing(Sizing.fixed(1)));
        }

        int labelWidth = Math.max(100, this.width - 120);
        if(!row.key().isEmpty()) {
            int keyWidth = Math.min(180, labelWidth / 3);
            LabelComponent key = UIComponents.label(Component.literal(row.key()));
            key.color(MUTED);
            key.horizontalSizing(Sizing.fixed(keyWidth));
            line.child(key);
            labelWidth -= keyWidth;
        }

        LabelComponent value = UIComponents.label(Component.literal(row.value()));
        value.horizontalSizing(Sizing.fixed(Math.max(80, labelWidth)));
        line.child(value);
        return line;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

}
