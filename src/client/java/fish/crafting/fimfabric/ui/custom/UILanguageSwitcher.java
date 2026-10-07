package fish.crafting.fimfabric.ui.custom;

import fish.crafting.fimfabric.connection.ConnectionManager;
import fish.crafting.fimfabric.rendering.custom.ScreenRenderContext;
import fish.crafting.fimfabric.ui.UIBox;
import fish.crafting.fimfabric.util.ClickContext;
import fish.crafting.fimfabric.util.ColorUtil;
import fish.crafting.fimfabric.util.SoundUtil;
import fish.crafting.fimfabric.util.render.FadeTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;

public class UILanguageSwitcher extends UIBox {

    private final FadeTracker textWhiteFadeTracker = new FadeTracker(0, 0, 0.2);
    private final FadeTracker fadeInTracker = new FadeTracker(0.2, 0, 0);

    public UILanguageSwitcher() {
        super(0, 0, 50, 16);
        hoverCursorClick();
    }

    @Override
    protected void render(ScreenRenderContext context) {
        Font textRenderer = Minecraft.getInstance().font;
        String text = "Language: ";

        renderHover(context);

        int y = renderY + renderHeight / 2 - textRenderer.lineHeight / 2;

        int alpha = Mth.lerpInt(fadeInTracker.alpha(255) / 255f, 0, 200);
        int clrWhite = ColorUtil.alpha(0xFFFFFFFF, alpha);

        context.drawText(
                textRenderer,
                text,
                renderX + 5,
                y,
                clrWhite,
                true
        );

        int width = textRenderer.width(text);
        float whiteProgress = textWhiteFadeTracker.alpha() / 255f;

        if(ConnectionManager.kotlin){
            int clrKotlin = ColorUtil.alpha(0xFF0065FF, alpha);

            context.drawText(
                    textRenderer,
                    "Kotlin",
                    renderX + 5 + width,
                    y,
                    ColorUtil.mix(clrKotlin, clrWhite, whiteProgress),
                    true
            );
        }else{
            int clrJava = ColorUtil.alpha(0xFFFF790C, alpha);
            context.drawText(
                    textRenderer,
                    "Java",
                    renderX + 5 + width,
                    y,
                    ColorUtil.mix(clrJava, clrWhite, whiteProgress),
                    true
            );
        }
    }

    @Override
    protected void onEnable(boolean wasDisabled) {
        if(wasDisabled){
            fadeInTracker.begin();
        }
    }

    @Override
    public void onClick(ClickContext context) {
        if(!context.isLeftClickPress()) return;

        ConnectionManager.kotlin = !ConnectionManager.kotlin;
        SoundUtil.clickSound();
        textWhiteFadeTracker.begin();
    }

    @Override
    public void onWindowResized(int width, int height) {
        move(width - this.width - 1, this.height + 4, 90, this.height);
    }
}
