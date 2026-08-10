package fish.crafting.fimfabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fish.crafting.fimfabric.rendering.RenderingManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//26.2
//
// GameRenderer#render no longer has a GuiGraphicsExtractor anywhere near it: GUI extraction moved to
// Gui#extractRenderState, which builds the extractor and feeds hud, overlay and screen into it. Its
// tail is the equivalent spot - everything else has been extracted, so FIM draws on top, screen open
// or not. The class keeps its old name so the mixin config stays the same across versions.
@Mixin(Gui.class)
public class GameRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void render(DeltaTracker tickCounter, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci,
                       @Local GuiGraphicsExtractor drawContext) {
        //Make sure this is ALWAYS getting called on EACH RENDER!!
        RenderingManager.get().renderOverlay(tickCounter, drawContext);
    }
}
