package fish.crafting.fimfabric.keybind;

import fish.crafting.fimfabric.ui.InterfaceManager;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.ChatScreen;

public class QuickActionsKeybind extends CustomKeybind{

    public QuickActionsKeybind() {
        super("quick_actions", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), KeybindCategory.IN_GAME_EDITOR);
    }

    @Override
    public void onPressed() {
        boolean returnToGame = false;

        if(Minecraft.getInstance().mouseHandler.isMouseGrabbed()) {
            Minecraft.getInstance().gui.setScreen(new ChatScreen("", false));
            returnToGame = true;
        }

        InterfaceManager.get().openQuickActions(returnToGame);
    }
}
