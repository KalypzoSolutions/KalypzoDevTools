package fish.crafting.fimfabric.keybind;

import fish.crafting.fimfabric.connection.ConnectionManager;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class SwitchToIntelliJKeybind extends CustomKeybind{

    public SwitchToIntelliJKeybind() {
        super("switch", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, KeybindCategory.INTELLIJ);
    }

    @Override
    public void onPressed() {
        ConnectionManager.get().focusIntelliJ();
    }
}
