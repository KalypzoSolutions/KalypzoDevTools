package fish.crafting.fimfabric.rendering;

import fish.crafting.fimfabric.util.McCompat;
import fish.crafting.fimfabric.rendering.custom.ScreenRenderContext;
import fish.crafting.fimfabric.tools.CustomTool;
import fish.crafting.fimfabric.tools.ToolManager;
import fish.crafting.fimfabric.ui.InterfaceManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

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

    public void renderOverlay(RenderTickCounter tickCounter, DrawContext mcDrawContext) {
        if(McCompat.hasOverlay()) return; //Reloading textures bugfix

        ScreenRenderContext context = new ScreenRenderContext(mcDrawContext);

        taskQueue.forEach(Runnable::run);
        taskQueue.clear();

        InformationFeedManager.get().render(context);
        InterfaceManager.get().render(context);

        context.draw();
    }

    public void renderInGameOverlay(DrawContext drawContext, RenderTickCounter tickCounter) {
        if(McCompat.hasOverlay()) return; //mhm

        ScreenRenderContext context = new ScreenRenderContext(drawContext);

        CustomTool<?> tool = ToolManager.get().getSelectedTool();
        if(tool != null) tool.render2D(context, tickCounter);
    }

    public void addTask(Runnable runnable){
        taskQueue.add(runnable);
    }
}
