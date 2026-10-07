package fish.crafting.fimfabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fish.crafting.fimfabric.rendering.RenderingManager;
import fish.crafting.fimfabric.ui.InterfaceManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    // Gui#extractRenderState builds the extractor and feeds hud, overlay and screen into it. At its
    // tail everything else has been extracted, so FIM draws on top, screen open or not.
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void render(DeltaTracker tickCounter, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci,
                       @Local GuiGraphicsExtractor drawContext) {
        //Make sure this is ALWAYS getting called on EACH RENDER!!
        RenderingManager.get().renderOverlay(tickCounter, drawContext);
    }

    @Inject(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    public void screen(Screen screen, CallbackInfo ci){
        InterfaceManager.get().onChatOpenState(screen instanceof ChatScreen);
    }

}
