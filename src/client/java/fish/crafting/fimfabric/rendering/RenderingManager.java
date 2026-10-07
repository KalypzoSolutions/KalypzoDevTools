package fish.crafting.fimfabric.rendering;

import fish.crafting.fimfabric.rendering.custom.ScreenRenderContext;
import fish.crafting.fimfabric.tools.CustomTool;
import fish.crafting.fimfabric.tools.ToolManager;
import fish.crafting.fimfabric.ui.InterfaceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;

import java.util.ArrayList;
import java.util.List;

public class RenderingManager {

    private static RenderingManager instance;
    private final List<Runnable> taskQueue = new ArrayList<>();

    private RenderingManager(){
        instance = this;
    }

    public static RenderingManager get(){
        return instance == null ? new RenderingManager() : instance;
    }

    public void renderOverlay(DeltaTracker tickCounter, GuiGraphicsExtractor mcDrawContext) {
        if(Minecraft.getInstance().gui.overlay() != null) return; //Reloading textures bugfix

        ScreenRenderContext context = new ScreenRenderContext(mcDrawContext);

        taskQueue.forEach(Runnable::run);
        taskQueue.clear();

        InformationFeedManager.get().render(context);
        InterfaceManager.get().render(context);
    }

    public void renderInGameOverlay(GuiGraphicsExtractor drawContext, DeltaTracker tickCounter) {
        if(Minecraft.getInstance().gui.overlay() != null) return; //mhm

        ScreenRenderContext context = new ScreenRenderContext(drawContext);

        CustomTool<?> tool = ToolManager.get().getSelectedTool();
        if(tool != null) tool.render2D(context, tickCounter);
    }

    public void addTask(Runnable runnable){
        taskQueue.add(runnable);
    }
}
