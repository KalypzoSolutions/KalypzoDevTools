package fish.crafting.fimfabric.minimessage;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Sizing;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * The whole editor in one piece: a toolbar over a MiniMessage text area. Drop it into any owo
 * layout:
 *
 * <pre>{@code
 * MiniMessageEditor editor = MiniMessageEditor.create(400, Sizing.fixed(120), "<gold>Hello", List.of());
 * editor.onChanged(text -> System.out.println(MiniMessage.parse(text).lines()));
 * parent.child(editor.layout());
 * }</pre>
 *
 * Screens that need several fields under one toolbar use {@link MiniMessageTextArea} and
 * {@link MiniMessageToolbar} directly.
 */
public final class MiniMessageEditor {

    private final FlowLayout layout;
    private final MiniMessageTextArea area;

    private MiniMessageEditor(FlowLayout layout, MiniMessageTextArea area){
        this.layout = layout;
        this.area = area;
    }

    /**
     * @param width Width in pixels the toolbar wraps at.
     * @param height Height of the text area.
     */
    public static @NotNull MiniMessageEditor create(int width, @NotNull Sizing height, @NotNull String initial,
                                                    @NotNull List<MiniMessageToolbar.Extra> extras){
        MiniMessageTextArea area = new MiniMessageTextArea(Sizing.fill(100), height, initial);
        area.setCharacterLimit(32767);

        FlowLayout layout = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        layout.gap(4);
        layout.child(MiniMessageToolbar.create(width, () -> area, extras));
        layout.child(area);
        return new MiniMessageEditor(layout, area);
    }

    public @NotNull FlowLayout layout(){
        return layout;
    }

    public @NotNull MiniMessageTextArea area(){
        return area;
    }

    public @NotNull String value(){
        return area.getValue();
    }

    public void value(@NotNull String value){
        area.replaceAll(value);
    }

    public void onChanged(@NotNull Consumer<String> listener){
        area.onChanged().subscribe(listener::accept);
    }

}
