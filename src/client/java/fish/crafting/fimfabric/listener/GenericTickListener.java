package fish.crafting.fimfabric.listener;

import fish.crafting.fimfabric.connection.ConnectionManager;
import fish.crafting.fimfabric.editor.values.EditorBoundingBox;
import fish.crafting.fimfabric.tools.Positioned;
import fish.crafting.fimfabric.tools.ToolManager;
import fish.crafting.fimfabric.tools.selector.WorldSelectorManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class GenericTickListener implements ClientTickEvents.EndTick {

    private int totalTicks = 0;

    @Override
    public void onEndTick(Minecraft client) {
        totalTicks++;
        WorldSelectorManager.get().updateSelectors();
        ConnectionManager.get().tick();

        /*Positioned editing = ToolManager.get().getEditing();
        if(editing == null){
            ToolManager.get().setEditing(new EditorBoundingBox(43.5, 115.5, -114.5, 54.5, 120.7, -117.5));
        }*/
    }
}
