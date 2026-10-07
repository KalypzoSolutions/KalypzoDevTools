package fish.crafting.fimfabric.tools.render;

import fish.crafting.fimfabric.client.FIMModClient;
import fish.crafting.fimfabric.rendering.custom.RenderContext3D;
import fish.crafting.fimfabric.tools.CustomTool;
import fish.crafting.fimfabric.tools.Positioned;
import fish.crafting.fimfabric.tools.ToolManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class ToolRenderingManager {

    private static final Identifier TOOL_DRAW = Identifier.fromNamespaceAndPath(FIMModClient.NAMESPACE, "tool_draw");
    private static ToolRenderingManager instance;

    private ToolRenderingManager() {
        instance = this;
    }

    public static ToolRenderingManager get(){
        return instance == null ? new ToolRenderingManager() : instance;
    }

    public void render(@NotNull RenderContext3D context){
        ToolManager toolManager = ToolManager.get();

        Positioned editing = toolManager.getEditing();
        if(editing == null) return;

        CustomTool<?> selectedTool = toolManager.getSelectedTool();
        if(selectedTool == null) return;

        selectedTool.handleRender(context, editing);
    }
}
