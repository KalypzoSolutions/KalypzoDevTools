package fish.crafting.fimfabric.tools.selector;

import fish.crafting.fimfabric.editor.values.EditorBoundingBox;
import fish.crafting.fimfabric.rendering.custom.RenderContext3D;
import fish.crafting.fimfabric.rendering.world.WorldRenderingManager;
import fish.crafting.fimfabric.tools.CustomTool;
import fish.crafting.fimfabric.tools.Positioned;
import fish.crafting.fimfabric.tools.ToolManager;
import fish.crafting.fimfabric.util.CursorPicking;
import fish.crafting.fimfabric.util.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static fish.crafting.fimfabric.util.DebugSettings.RENDER_SELECTOR_HITBOXES;

/**
 * Handles all the Boxes in the world that the player can interact with.
 * Example: Bounding Boxes for Editor Tools
 */
public class WorldSelectorManager {

    public static int LAST_RENDER_FRAME = 0;
    private static WorldSelectorManager instance;
    private final Set<WorldSelector> activeSelectors = new HashSet<>();
    private WorldSelector hovered = null;
    private WorldSelector holding = null;

    private WorldSelectorManager(){
        instance = this;
    }

    public static WorldSelectorManager get(){
        return instance == null ? new WorldSelectorManager() : instance;
    }

    private void refreshActiveSelectors(){
        CustomTool<?> selectedTool = ToolManager.get().getSelectedTool();
        if(selectedTool != null) {
            List<WorldSelector> selectors = selectedTool.getSelectors();
            if(selectors != null) {
                selectors.forEach(WorldSelector::update);
            }
        }

        Positioned editing = ToolManager.get().getEditing();
        if(editing != null) {
            editing.selector().update();

            if(editing instanceof EditorBoundingBox box){
                box.handleSelectorUpdate();
            }
        }
    }

    private ScreenSelector getHoveredScreen(){
        Vec3 from = Minecraft.getInstance().gameRenderer.mainCamera().position();
        Vec3 to = CursorPicking.createEndPoint(1000);

        Double lowestDistance = null;
        ScreenSelector hovered = null;

        for (WorldSelector activeSelector : activeSelectors) {
            if(!(activeSelector instanceof ScreenSelector selector)) continue;

            Vec3 raycast = selector.raycast(from, to);
            if(raycast == null) continue;

            double d = raycast.distanceToSqr(from);
            if(lowestDistance == null || d < lowestDistance) {
                lowestDistance = d;
                hovered = selector;
            }
        }

        return hovered;
    }

    private WorldSelector getHoveredWorld(){
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return null;

        float tickDelta = RenderUtils.tickDelta();
        Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();

        Vec3 vec3d = player.getEyePosition(tickDelta);
        Vec3 vec3d2 = vec3d.add(player.getViewVector(tickDelta).scale(100));

        Double lowestDistance = null;
        WorldSelector hovered = null;

        for (WorldSelector selector : activeSelectors) {
            if(!selector.canBeSelectedInCamera()) continue;

            Vec3 raycast = selector.raycast(vec3d, vec3d2);
            if(raycast == null) continue;

            double d = raycast.distanceToSqr(camera);
            if(lowestDistance == null || d < lowestDistance) {
                lowestDistance = d;
                hovered = selector;
            }
        }

        return hovered;
    }

    public void updateHover(){
        WorldSelector hovered = null;
        if(!Minecraft.getInstance().mouseHandler.isMouseGrabbed()){
            hovered = getHoveredScreen();
        }

        //Didn't hit anything on screenspace, get from world
        if(hovered == null){
            hovered = getHoveredWorld();
        }

        if(this.hovered == hovered) return;

        if(this.hovered != null){
            this.hovered.onHoverStatus(false);
        }

        if(hovered != null){
            hovered.onHoverStatus(true);
        }

        this.hovered = hovered;
    }

    public void renderSelectors(@NotNull RenderContext3D context){
        context.push();
        context.translateCamera();

        context.setLineWidth(4f);

        for (var selector : activeSelectors) {
            context.renderBoxOutline(
                    selector.getBox(),
                    1,
                    1,
                    1,
                    RENDER_SELECTOR_HITBOXES ? 1f : 0.3f
            );
        }

        context.pop();
    }

    public void updateSelectors() {
        refreshActiveSelectors();

        //-------------------------

        int now = WorldRenderingManager.RENDER_FRAME;

        Iterator<WorldSelector> iterator = activeSelectors.iterator();
        while(iterator.hasNext()) {
            WorldSelector next = iterator.next();
            if(next.lastRenderFrame != now){
                remove(next);
                next.onDisabled();
                iterator.remove();
            }
        }

        LAST_RENDER_FRAME = now;
    }

    public @Nullable WorldSelector getMainActiveSelector(){
        if(holding != null) return holding;

        return hovered;
    }

    /**
     * @return True, if the click should be cancelled.
     */
    public boolean handlePress(int button, int action, int mods){
        if(action == 0){ //STOP PRESS
            if(holding != null){
                holding.handleDepress();
            }

            holding = null;
        }else if(action == 1 && hovered != null){ //START PRESS
            if (holding != null) {
                holding.handleDepress();
            }

            holding = hovered;
            holding.handlePress(button, mods);
            return true;
        }

        return false;
    }

    private void remove(WorldSelector selector){
        if(hovered == selector){
            hovered = null;
        }

        if(holding == selector){
            holding = null;
        }
    }


    public void add(WorldSelector worldSelector) {
        boolean add = this.activeSelectors.add(worldSelector);
        if(add) {
            worldSelector.onEnabled();
        }
    }
}
