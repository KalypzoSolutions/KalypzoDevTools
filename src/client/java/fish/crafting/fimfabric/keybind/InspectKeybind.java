package fish.crafting.fimfabric.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import fish.crafting.fimfabric.inspect.item.ItemInspectManager;
import org.lwjgl.glfw.GLFW;

/**
 * Inspects the block in the crosshair or the held item. Inside of containers the same key inspects
 * the item under the cursor, which the screen hook of the manager takes care of.
 */
public class InspectKeybind extends CustomKeybind {

    public InspectKeybind() {
        super("inspect", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, KeybindCategory.IN_GAME_EDITOR);
    }

    @Override
    public void onPressed() {
        ItemInspectManager.get().inspectInWorld();
    }
}
