package fish.crafting.fimfabric.editor.values;

import fish.crafting.fimfabric.editor.EditorReference;
import fish.crafting.fimfabric.editor.Referenced;
import fish.crafting.fimfabric.settings.VectorSettings;
import fish.crafting.fimfabric.tools.MoveTool;
import fish.crafting.fimfabric.tools.Positioned;
import fish.crafting.fimfabric.tools.ToolManager;
import fish.crafting.fimfabric.tools.selector.WorldSelector;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class EditorVector implements Positioned, Referenced {

    private final WorldSelector selector = new Selector();
    public @NotNull Vec3 vector = new Vec3(0, 0, 0);
    private @NotNull AABB box = new AABB(0, 0, 0, 0, 0, 0);
    private final EditorReference reference = new EditorReference();
    public int lastRenderFrame = 0;

    public EditorVector(){
        updateBox();
    }

    public EditorVector(double x, double y, double z){
        setVector(x, y, z);
    }

    public void setVector(double x, double y, double z){
        this.vector = new Vec3(x, y, z);
        updateBox();
    }

    public void updateBox(){
        double d = VectorSettings.renderSize();
        this.box = new AABB(
                vector.x - d,
                vector.y - d,
                vector.z - d,
                vector.x + d,
                vector.y + d,
                vector.z + d
        );
    }

    @Override
    public WorldSelector selector() {
        return this.selector;
    }

    @Override
    public Vec3 getPos() {
        return vector;
    }

    @Override
    public void setPos(double x, double y, double z) {
        setVector(x, y, z);
    }

    @Override
    public EditorReference reference() {
        return reference;
    }

    private class Selector extends WorldSelector {

        @Override
        protected void onPress(int button, int mods) {
            toolCallback(true);
        }

        @Override
        protected void onUnPress() {
            toolCallback(false);
        }

        private void toolCallback(boolean press){
            if(ToolManager.get().getEditing() != EditorVector.this) return;
            if(ToolManager.get().getSelectedTool() instanceof MoveTool moveTool){
                moveTool.vectorClickCallback(EditorVector.this, press);
            }
        }

        @Override
        public AABB getBox() {
            return box;
        }
    }
}
