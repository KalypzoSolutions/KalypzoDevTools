package fish.crafting.fimfabric.mixin;

import fish.crafting.fimfabric.connection.packets.F2IMinecraftFocusedPacket;
import fish.crafting.fimfabric.ui.InterfaceManager;
import fish.crafting.fimfabric.util.WindowUtil;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "onFramebufferResize", at = @At("HEAD"))
    public void onWindowResize(long window, int width, int height, CallbackInfo ci){
        var g = WindowUtil.guiScale();
        InterfaceManager.get().onResize(width / g, height / g);
    }

    @Inject(method = "onFocus", at = @At("HEAD"))
    public void onFocus(long window, boolean focused, CallbackInfo ci){
        if(focused && window == Minecraft.getInstance().getWindow().handle()) {
            new F2IMinecraftFocusedPacket().send();
        }
    }

}
