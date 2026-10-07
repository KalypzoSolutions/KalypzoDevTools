package fish.crafting.fimfabric.titleoffset;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

/**
 * Slider for a single shift. It covers as many pixels of shift as it is wide, so dragging it by
 * one pixel moves the title by one pixel. The covered range follows the value around.
 */
public class OffsetSlider extends SliderWidget {

    //Width of the handle, which the slider can't travel
    private static final int HANDLE_WIDTH = 8;

    private final TitleOffsetModel.Slot slot;
    private final Runnable onChange;
    private final int steps;
    private int min;

    public OffsetSlider(int x, int y, int width, int height, TitleOffsetModel.Slot slot, Runnable onChange){
        super(x, y, width, height, Text.empty(), 0.5);
        this.slot = slot;
        this.onChange = onChange;
        this.steps = width - HANDLE_WIDTH;

        center();
        updateMessage();
    }

    private void center(){
        min = slot.getValue() - steps / 2;
        //setValue is private before 26.2
        value = (slot.getValue() - min) / (double) steps;
    }

    /**
     * Moves the shift by a fixed amount, past the ends of the slider if need be.
     */
    public void nudge(int delta){
        slot.setValue(slot.getValue() + delta);
        if(slot.getValue() < min || slot.getValue() > min + steps){
            center();
        }else{
            value = (slot.getValue() - min) / (double) steps;
        }

        updateMessage();
        onChange.run();
    }

    @Override
    protected void applyValue(){
        slot.setValue(min + (int) Math.round(value * steps));
        onChange.run();
    }

    @Override
    protected void updateMessage(){
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        String number = "#" + (slot.getIndex() + 1) + "  " + slot.getValue();
        String label = slot.getLabel();

        //Long labels would start scrolling inside the slider
        String message = label.isEmpty() ? number : number + "  " + label;
        while(!label.isEmpty() && textRenderer.getWidth(message) > steps){
            label = label.substring(0, label.length() - 1);
            message = label.isEmpty() ? number : number + "  " + label + "..";
        }

        setMessage(Text.literal(message));
    }

}
