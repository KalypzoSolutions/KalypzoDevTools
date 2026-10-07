package fish.crafting.fimfabric.keybind;

import fish.crafting.fimfabric.tools.ToolManager;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class ConfirmEditKeybind extends CustomKeybind{

    public ConfirmEditKeybind() {
        super("confirm_edit", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, KeybindCategory.IN_GAME_EDITOR);
    }

    @Override
    public void onPressed() {
        ToolManager.get().confirmEdit();
    }
}
