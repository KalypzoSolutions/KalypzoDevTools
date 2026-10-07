package fish.crafting.fimfabric.rendering.custom;

import fish.crafting.fimfabric.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class ScreenRenderContext {

    @NotNull
    private final GuiGraphicsExtractor context;

    public ScreenRenderContext(@NotNull GuiGraphicsExtractor context){
        this.context = context;
    }

    public void push(){
        context.pose().pushMatrix();
    }

    public void pop(){
        context.pose().popMatrix();
    }

    public void drawGuiTexture(@NotNull Identifier identifier, int x, int y, int width, int height, int color){
        context.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                identifier,
                x, y,
                width, height,
                color
        );
    }

    public void translate(double x, double y){
        context.pose().translate((float) x, (float) y);
    }

    public void drawText(String text, int x, int y, int color, boolean shadow){
        drawText(textRenderer(), text, x, y, color, shadow);
    }

    public void drawText(Font renderer, String text, int x, int y, int color, boolean shadow){
        context.text(
                renderer,
                text,
                x,
                y,
                color,
                shadow
        );
    }

    public void drawCenteredTextWithShadow(String text, int centerX, int y, int color){
        context.centeredText(
                textRenderer(),
                text,
                centerX,
                y,
                color
        );
    }

    //Layers only go up; elements drawn later end up on top anyway.
    public void nextLayer(){
        context.guiRenderState.up();
    }

    public void fill(int x1, int y1, int x2, int y2, int color){
        context.fill(x1, y1, x2, y2, color);
    }

    public void fillYGradient(int x1, int y1, int x2, int y2, int color1, int color2){
        context.fillGradient(x1, y1, x2, y2, color1, color2);
    }

    /**
     * Draws a custom gradient box.
     * It looks like this:
     * GRADIENT (FADE IN)
     * FILL
     * GRADIENT (FADE OUT)
     */
    public void drawGradientBox(int fillX, int fillY, int fillWidth, int fillHeight, int gradientHeight, int color, boolean outline){
        int emptyColor = ColorUtil.alpha(color, 0);

        fillYGradient(fillX,
                fillY - gradientHeight,
                fillX + fillWidth,
                fillY,
                emptyColor,
                color);

        fill(fillX,
                fillY,
                fillX + fillWidth,
                fillY + fillHeight,
                color);

        fillYGradient(fillX,
                fillY + fillHeight,
                fillX + fillWidth,
                fillY + fillHeight + gradientHeight,
                color,
                emptyColor);

        if(outline) {
            int lineWidth = 2;
            int offset = gradientHeight / 3;

            fillY += offset;
            fillHeight -= offset * 2;
            gradientHeight += offset;

            drawGradientBox(
                    fillX - lineWidth,
                    fillY,
                    lineWidth,
                    fillHeight,
                    gradientHeight,
                    0xFFFFFFFF,
                    false
            );

            drawGradientBox(
                    fillX + fillWidth,
                    fillY,
                    lineWidth,
                    fillHeight,
                    gradientHeight,
                    0xFFFFFFFF,
                    false
            );
        }
    }

    //GuiGraphicsExtractor has no line/border helpers, so they are filled by hand.
    public void drawVerticalLine(int x, int y1, int y2, int color){
        context.fill(x, Math.min(y1, y2) + 1, x + 1, Math.max(y1, y2), color);
    }

    public void drawHorizontalLine(int x1, int x2, int y, int color){
        context.fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, color);
    }

    public void drawBorder(int x, int y, int width, int height, int color){
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public int fontHeight() {
        return textRenderer().lineHeight;
    }

    private static Font textRenderer(){
        return Minecraft.getInstance().font;
    }

}
