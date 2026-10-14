package fish.crafting.fimfabric.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import fish.crafting.fimfabric.inspect.item.ItemInspectManager;
import org.lwjgl.glfw.GLFW;

/**
 * Opens the name and lore editor for the held item, or for the item under the cursor in containers.
 */
public class EditItemKeybind extends CustomKeybind {

    public EditItemKeybind() {
        super("edit_item", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KeybindCategory.IN_GAME_EDITOR);
    }

    @Override
    public void onPressed() {
        ItemInspectManager.get().editInWorld();
    }
}
