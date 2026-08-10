package fish.crafting.fimfabric.mixin;

import fish.crafting.fimfabric.ui.InterfaceManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//26.2
//
// Screen management moved from Minecraft onto Gui.
@Mixin(Gui.class)
public class MinecraftClientMixin {

    @Inject(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    public void screen(Screen screen, CallbackInfo ci){
        InterfaceManager.get().onChatOpenState(screen instanceof ChatScreen);
    }

}
