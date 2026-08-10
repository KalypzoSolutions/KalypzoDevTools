package fish.crafting.fimfabric.mixin;

import fish.crafting.fimfabric.tools.ToolManager;
import fish.crafting.fimfabric.tools.selector.WorldSelectorManager;
import fish.crafting.fimfabric.ui.InterfaceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//26.2
//
// Mouse is MouseHandler and the button callback takes a MouseButtonInfo record instead of the raw
// button/mods ints.
@Mixin(MouseHandler.class)
public class MouseMixin {

    @Inject(method = "onMove", at = @At("HEAD"))
    public void cursorPos(long window, double x, double y, CallbackInfo ci){
        InterfaceManager.get().handleCursorPos((int) x, (int) y);
    }

    @Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (window != Minecraft.getInstance().getWindow().handle()) return;

        boolean cancel = false;
        MouseHandler mouse = (MouseHandler) (Object) this;
        if(!mouse.isMouseGrabbed()){
            InterfaceManager.get().handleClick(buttonInfo.button(), action, buttonInfo.modifiers());
        }else{
            cancel = WorldSelectorManager.get().handlePress(buttonInfo.button(), action, buttonInfo.modifiers());
        }

        if(cancel){
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window != Minecraft.getInstance().getWindow().handle()) return;

        boolean cancel = false;
        MouseHandler mouse = (MouseHandler) (Object) this;
        if (!mouse.isMouseGrabbed()) {
            InterfaceManager.get().handleScroll(vertical);
        }else{
            cancel = ToolManager.get().onScroll(vertical);
        }

        if(cancel) {
            ci.cancel();
        }
    }

    @Inject(method = "grabMouse", at = @At("HEAD"))
    public void lock(CallbackInfo ci){
        InterfaceManager.get().handleMouseLock(true);
    }

    @Inject(method = "releaseMouse", at = @At("HEAD"))
    public void unlock(CallbackInfo ci){
        InterfaceManager.get().handleMouseLock(false);
    }

}
