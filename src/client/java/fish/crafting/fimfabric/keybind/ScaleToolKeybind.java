package fish.crafting.fimfabric.keybind;

import fish.crafting.fimfabric.tools.EditorTools;
import fish.crafting.fimfabric.tools.ToolManager;
import com.mojang.blaze3d.platform.InputConstants;

public class ScaleToolKeybind extends CustomKeybind{

    public ScaleToolKeybind() {
        super("scale_tool", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), KeybindCategory.IN_GAME_EDITOR);
    }

    @Override
    public void onPressed() {
        ToolManager.get().setSelectedTool(EditorTools.SCALE);
    }
}
