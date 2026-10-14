package fish.crafting.fimfabric.minimessage;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The buttons of a MiniMessage editor: decorations, colors, effects, undo and redo. They act on
 * whichever text area the supplier hands out, so one toolbar can serve several fields.
 */
public final class MiniMessageToolbar {

    /**
     * An extra button.
     *
     * @param close Closing tag, null for a tag that is just put in.
     */
    public record Extra(@NotNull String label, @NotNull String open, @Nullable String close) {}

    private static final String[][] PALETTE = {
            {"black", "000000"}, {"dark_blue", "0000AA"}, {"dark_green", "00AA00"}, {"dark_aqua", "00AAAA"},
            {"dark_red", "AA0000"}, {"dark_purple", "AA00AA"}, {"gold", "FFAA00"}, {"gray", "AAAAAA"},
            {"dark_gray", "555555"}, {"blue", "5555FF"}, {"green", "55FF55"}, {"aqua", "55FFFF"},
            {"red", "FF5555"}, {"light_purple", "FF55FF"}, {"yellow", "FFFF55"}, {"white", "FFFFFF"}
    };

    private final Supplier<@Nullable MiniMessageTextArea> target;
    private final int width;
    private final List<ButtonComponent> undoRedo = new ArrayList<>();

    private MiniMessageToolbar(Supplier<@Nullable MiniMessageTextArea> target, int width){
        this.target = target;
        this.width = width;
    }

    /**
     * @param width Room the rows may use before they wrap.
     * @param target The field the buttons work on right now.
     */
    public static @NotNull FlowLayout create(int width, @NotNull Supplier<@Nullable MiniMessageTextArea> target, @NotNull List<Extra> extras){
        return new MiniMessageToolbar(target, width).build(extras);
    }

    private FlowLayout build(List<Extra> extras){
        FlowLayout toolbar = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        toolbar.gap(2);

        //Decorations and history
        List<ButtonComponent> row = new ArrayList<>();
        row.add(button(Component.literal("B").withStyle(Style.EMPTY.withBold(true)), "Bold (Ctrl+B)", () -> format("<bold>", "</bold>")));
        row.add(button(Component.literal("I").withStyle(Style.EMPTY.withItalic(true)), "Italic (Ctrl+I)", () -> format("<italic>", "</italic>")));
        row.add(button(Component.literal("U").withStyle(Style.EMPTY.withUnderlined(true)), "Underlined (Ctrl+U)", () -> format("<underlined>", "</underlined>")));
        row.add(button(Component.literal("S").withStyle(Style.EMPTY.withStrikethrough(true)), "Strikethrough", () -> format("<strikethrough>", "</strikethrough>")));
        row.add(button(Component.literal("?").withStyle(Style.EMPTY.withObfuscated(true)), "Obfuscated", () -> format("<obfuscated>", "</obfuscated>")));
        row.add(button(Component.literal("Clear"), "Remove all tags from the selection", () -> run(MiniMessageTextArea::stripSelection)));
        row.add(button(Component.literal("Undo"), "Ctrl+Z", () -> run(MiniMessageTextArea::undo)));
        row.add(button(Component.literal("Redo"), "Ctrl+Y", () -> run(MiniMessageTextArea::redo)));
        toolbar.child(rows(row));

        //Colors
        List<ButtonComponent> colors = new ArrayList<>();
        for (String[] color : PALETTE) {
            Component swatch = Component.literal("■").withColor(Integer.parseInt(color[1], 16));
            ButtonComponent button = button(swatch, color[0], () -> format("<" + color[0] + ">", "</" + color[0] + ">"));
            button.horizontalSizing(Sizing.fixed(18));
            colors.add(button);
        }
        colors.add(button(Component.literal("Hex"), "<#rrggbb>", () -> format("<#ff8800>", "</#ff8800>")));
        colors.add(button(Component.literal("Gradient"), "<gradient:#ff5555:#5555ff>", () -> format("<gradient:#ff5555:#5555ff>", "</gradient>")));
        colors.add(button(Component.literal("Rainbow"), "<rainbow>", () -> format("<rainbow>", "</rainbow>")));
        toolbar.child(rows(colors));

        if(!extras.isEmpty()){
            List<ButtonComponent> more = new ArrayList<>();
            for (Extra extra : extras) {
                more.add(button(Component.literal(extra.label()), extra.open(), () -> {
                    MiniMessageTextArea area = target.get();
                    if(area == null) return;

                    if(extra.close() == null) area.insertAtCursor(extra.open());
                    else area.format(extra.open(), extra.close());
                    area.refocus();
                }));
            }
            toolbar.child(rows(more));
        }

        return toolbar;
    }

    private void format(String open, String close){
        run(area -> {
            area.format(open, close);
            return true;
        });
    }

    private void run(java.util.function.Function<MiniMessageTextArea, Boolean> action){
        MiniMessageTextArea area = target.get();
        if(area == null) return;

        action.apply(area);
        area.refocus();
    }

    private void run(java.util.function.Consumer<MiniMessageTextArea> action){
        run(area -> {
            action.accept(area);
            return true;
        });
    }

    private ButtonComponent button(Component label, String tooltip, Runnable action){
        ButtonComponent button = UIComponents.button(label, b -> action.run());
        int textWidth = Minecraft.getInstance().font.width(label);
        button.horizontalSizing(Sizing.fixed(Math.max(18, textWidth + 10)));
        button.verticalSizing(Sizing.fixed(18));
        button.tooltip(Component.literal(tooltip));
        return button;
    }

    /**
     * Lays buttons out in rows that break before they run out of the available width.
     */
    private FlowLayout rows(List<ButtonComponent> buttons){
        FlowLayout rows = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        rows.gap(2);

        FlowLayout row = null;
        int used = 0;
        for (ButtonComponent button : buttons) {
            int buttonWidth = button.horizontalSizing().get().value;
            if(row == null || used + buttonWidth > width){
                row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
                row.gap(2);
                rows.child(row);
                used = 0;
            }

            row.child(button);
            used += buttonWidth + 2;
        }
        return rows;
    }

}
