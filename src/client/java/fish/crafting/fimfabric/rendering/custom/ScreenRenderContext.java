package fish.crafting.fimfabric.rendering.custom;

import fish.crafting.fimfabric.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
//#if MC>=12106
import net.minecraft.client.gl.RenderPipelines;
//#endif
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class ScreenRenderContext {

    @NotNull
    private final DrawContext context;

    public ScreenRenderContext(@NotNull DrawContext context){
        this.context = context;
    }

    public void push(){
        //#if MC<12106
        //$$ context.getMatrices().push();
        //#else
        context.getMatrices().pushMatrix();
        //#endif
    }

    public void pop(){
        //#if MC<12106
        //$$ context.getMatrices().pop();
        //#else
        context.getMatrices().popMatrix();
        //#endif
    }

    public void drawGuiTexture(@NotNull Identifier identifier, int x, int y, int width, int height, int color){
        context.drawGuiTexture(
                //#if MC<12106
                //$$ RenderLayer::getGuiTextured,
                //#else
                RenderPipelines.GUI_TEXTURED,
                //#endif
                identifier,
                x, y,
                width, height,
                color
        );
    }

    public void translate(double x, double y){
        translate(x, y, 0);
    }

    public void translate(double x, double y, double z){
        //#if MC<12106
        //$$ context.getMatrices().translate(x, y, z);
        //#else
        context.getMatrices().translate((float) x, (float) y);
        //#endif
    }

    //This method used to be needed before 1.21.6, to finalize draws
    public void draw(){
        //#if MC<12106
        //$$ context.draw();
        //#endif
    }

    public void drawText(String text, int x, int y, int color, boolean shadow){
        drawText(textRenderer(), text, x, y, color, shadow);
    }

    public void drawText(TextRenderer renderer, String text, int x, int y, int color, boolean shadow){
        context.drawText(
                renderer,
                text,
                x,
                y,
                color,
                shadow
        );
    }

    public void drawCenteredTextWithShadow(String text, int centerX, int y, int color){
        context.drawCenteredTextWithShadow(
                textRenderer(),
                text,
                centerX,
                y,
                color
        );
    }

    public void nextLayer(){
        //#if MC>=260200
        //$$ context.guiRenderState.up();
        //#elseif MC>=12106
        context.state.goUpLayer();
        //#else
        //$$ translate(0, 0, 1);
        //#endif
    }

    public void previousLayer(){
        //#if MC>=260200
        //$$ //26.2 only knows how to go up a layer; later elements draw on top anyway.
        //#elseif MC>=12106
        context.state.goDownLayer();
        //#else
        //$$ translate(0, 0, -1);
        //#endif
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

    //26.2 dropped the line/border helpers, so they are filled by hand there.
    public void drawVerticalLine(int x, int y1, int y2, int color){
        //#if MC>=260200
        //$$ context.fill(x, Math.min(y1, y2) + 1, x + 1, Math.max(y1, y2), color);
        //#else
        context.drawVerticalLine(x, y1, y2, color);
        //#endif
    }

    public void drawHorizontalLine(int x1, int x2, int y, int color){
        //#if MC>=260200
        //$$ context.fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, color);
        //#else
        context.drawHorizontalLine(x1, x2, y, color);
        //#endif
    }

    public void drawBorder(int x, int y, int width, int height, int color){
        //#if MC>=260200
        //$$ context.fill(x, y, x + width, y + 1, color);
        //$$ context.fill(x, y + height - 1, x + width, y + height, color);
        //$$ context.fill(x, y + 1, x + 1, y + height - 1, color);
        //$$ context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
        //#else
        context.drawBorder(x, y, width, height, color);
        //#endif
    }

    public int fontHeight() {
        return textRenderer().fontHeight;
    }

    private static TextRenderer textRenderer(){
        return MinecraftClient.getInstance().textRenderer;
    }

}
