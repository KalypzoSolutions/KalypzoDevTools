package fish.crafting.fimfabric.titleoffset;

import fish.crafting.fimfabric.mixin.ScreenAccessor;
import fish.crafting.fimfabric.rendering.InformationFeedManager;
import fish.crafting.fimfabric.util.KeyUtil;
import fish.crafting.fimfabric.util.McCompat;
import fish.crafting.fimfabric.util.SoundUtil;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Puts a slider for every shift of a container title next to the container, so the shifts can be
 * tuned on the open screen instead of being guessed on the server.
 */
public class TitleOffsetEditor {

    private static final int MARGIN = 4;
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_PITCH = 18;
    //Width of a vanilla container, which the sliders should stay clear of
    private static final int CONTAINER_WIDTH = 176;

    private static TitleOffsetEditor instance;

    private @Nullable Screen screen = null;
    //Outlives the screen, so the values survive the server opening the same title again
    private @Nullable TitleOffsetModel model = null;
    private boolean open = false;
    private final List<ClickableWidget> widgets = new ArrayList<>();
    private final List<OffsetSlider> sliders = new ArrayList<>();

    private TitleOffsetEditor(){
        instance = this;
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if(screen instanceof HandledScreen<?>) onInit(screen);
        });
    }

    public static TitleOffsetEditor get(){
        return instance == null ? new TitleOffsetEditor() : instance;
    }

    /**
     * Runs when a screen opens and again whenever it is resized, which drops all of its widgets.
     */
    private void onInit(Screen screen){
        if(screen != this.screen){
            TitleOffsetModel fresh = new TitleOffsetModel(screen.getTitle());
            if(fresh.getSlots().isEmpty()) return;

            boolean reopened = model != null && model.isModified() && fresh.hasSameOriginals(model);
            if(reopened) fresh.copyValuesFrom(model);

            this.screen = screen;
            this.model = fresh;
            if(reopened) applyTitle();
        }

        //Screen events only last until the next init
        ScreenMouseEvents.allowMouseScroll(screen).register(this::allowScroll);
        ScreenEvents.remove(screen).register(removed -> {
            if(removed == this.screen) this.screen = null;
        });

        widgets.clear();
        rebuild();
    }

    private void rebuild(){
        if(screen == null || model == null) return;

        McCompat.screenWidgets(screen).removeAll(widgets);
        widgets.clear();
        sliders.clear();

        add(ButtonWidget.builder(Text.literal("Title offsets"), button -> toggle())
                .dimensions(MARGIN, MARGIN, 80, ROW_HEIGHT).build());
        if(!open) return;

        add(ButtonWidget.builder(Text.literal("Copy"), button -> copy())
                .dimensions(MARGIN + 82, MARGIN, 40, ROW_HEIGHT).build());
        add(ButtonWidget.builder(Text.literal("Reset"), button -> reset())
                .dimensions(MARGIN + 124, MARGIN, 40, ROW_HEIGHT).build());

        int width = Math.clamp((screen.width - CONTAINER_WIDTH) / 2 - MARGIN * 2, 80, 168);
        int top = MARGIN + ROW_PITCH;
        int rows = Math.max(1, (screen.height - top - MARGIN) / ROW_PITCH);

        //Left of the container first, whatever doesn't fit goes to the right of it
        for(TitleOffsetModel.Slot slot : model.getSlots()){
            boolean left = slot.getIndex() < rows;
            int x = left ? MARGIN : screen.width - MARGIN - width;
            int y = top + (left ? slot.getIndex() : slot.getIndex() - rows) * ROW_PITCH;

            OffsetSlider slider = new OffsetSlider(x, y, width, ROW_HEIGHT, slot, this::applyTitle);
            sliders.add(slider);
            add(slider);
        }
    }

    private void add(ClickableWidget widget){
        widgets.add(widget);
        McCompat.screenWidgets(screen).add(widget);
    }

    private void applyTitle(){
        if(screen == null || model == null) return;

        ((ScreenAccessor) screen).setFimTitle(model.build());
    }

    /**
     * Containers never pass scrolling on to their widgets, so it is picked up here.
     */
    private boolean allowScroll(Screen screen, double mouseX, double mouseY, double horizontalAmount, double verticalAmount){
        double amount = verticalAmount != 0 ? verticalAmount : horizontalAmount;
        if(!open || amount == 0) return true;

        for(OffsetSlider slider : sliders){
            if(!slider.isMouseOver(mouseX, mouseY)) continue;

            slider.nudge((amount > 0 ? 1 : -1) * (KeyUtil.isShiftPressed() ? 10 : 1));
            return false;
        }
        return true;
    }

    private void toggle(){
        open = !open;
        rebuild();
    }

    private void copy(){
        if(model == null) return;

        MinecraftClient.getInstance().keyboard.setClipboard(model.export());
        InformationFeedManager.success("Copied!", false);
        SoundUtil.clickSound();
    }

    private void reset(){
        if(model == null) return;

        model.reset();
        applyTitle();
        rebuild();
    }

}
