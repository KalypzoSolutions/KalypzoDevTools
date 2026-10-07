package fish.crafting.fimfabric.mixin;

import fish.crafting.fimfabric.rendering.RenderingManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// `Gui` is the screen/overlay manager; the actual HUD is `Hud`, which extracts its render state
// instead of rendering directly.
@Mixin(Hud.class)
public class HudMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    public void render(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci){
        RenderingManager.get().renderInGameOverlay(context, tickCounter);
    }

}
