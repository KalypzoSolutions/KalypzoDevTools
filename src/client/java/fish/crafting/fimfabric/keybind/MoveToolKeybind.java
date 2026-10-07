package fish.crafting.fimfabric.keybind;

import fish.crafting.fimfabric.tools.EditorTools;
import fish.crafting.fimfabric.tools.ToolManager;
import com.mojang.blaze3d.platform.InputConstants;

public class MoveToolKeybind extends CustomKeybind{

    public MoveToolKeybind() {
        super("move_tool", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), KeybindCategory.IN_GAME_EDITOR);
    }

    @Override
    public void onPressed() {
        ToolManager.get().setSelectedTool(EditorTools.MOVE);
    }
}
