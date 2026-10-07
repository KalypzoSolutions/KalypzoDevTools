package fish.crafting.fimfabric.keybind;

import fish.crafting.fimfabric.tools.EditorTools;
import fish.crafting.fimfabric.tools.MoveTool;
import fish.crafting.fimfabric.tools.ToolManager;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class PreviousSnappingKeybind extends CustomKeybind{

    public PreviousSnappingKeybind() {
        super("snapping_previous", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_SUBTRACT, KeybindCategory.IN_GAME_EDITOR);
    }

    @Override
    public void onPressed() {
        if(ToolManager.get().getEditing() != null && ToolManager.get().getSelectedTool() == EditorTools.MOVE) {
            MoveTool.moveSnapping(-1);
        }
    }
}
