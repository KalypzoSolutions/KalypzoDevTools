package fish.crafting.fimfabric.editor.values;

import fish.crafting.fimfabric.editor.EditorReference;
import fish.crafting.fimfabric.editor.Referenced;
import fish.crafting.fimfabric.settings.BoundingBoxSettings;
import fish.crafting.fimfabric.tools.*;
import fish.crafting.fimfabric.tools.selector.ScreenSelector;
import fish.crafting.fimfabric.tools.selector.WorldSelector;
import fish.crafting.fimfabric.util.Pair;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class EditorBoundingBox implements Referenced, PosScaled {

    public Vec3 min, max;

    private final WorldSelector selector = new Selector();
    public @NotNull Vec3 center = new Vec3(0, 0, 0);

    private @NotNull AABB renderBox = new AABB(0, 0, 0, 0, 0, 0);
    private final EditorReference reference = new EditorReference();
    public int lastRenderFrame = 0;
    private final StretchSelector[] edgeSelectors = new StretchSelector[12];

    public EditorBoundingBox(double x1, double y1, double z1, double x2, double y2, double z2){
        for (int i = 0; i < edgeSelectors.length; i++) {
            edgeSelectors[i] = new StretchSelector();
        }

        setValues(x1, y1, z1, x2, y2, z2);
    }

    public void setValues(double x1, double y1, double z1, double x2, double y2, double z2){
        min = new Vec3(
                Math.min(x1, x2),
                Math.min(y1, y2),
                Math.min(z1, z2));

        max = new Vec3(
                Math.max(x1, x2),
                Math.max(y1, y2),
                Math.max(z1, z2));

        updateEdges();
        updateCenter();
    }

    private void updateEdges(){
        int i = 0;
        for (Pair<Vec3, Vec3> edge : getEdges()) {
            edgeSelectors[i++].update(edge.getLeft(), edge.getRight());
        }
    }

    public Pair<Vec3, Vec3>[] getEdges(){
        Pair<Vec3, Vec3>[] edges = new Pair[12];

        double x = min.x;
        double y = min.y;
        double z = min.z;
        double x2 = max.x;
        double y2 = max.y;
        double z2 = max.z;

        edges[0] = createXEdge(x, x2, y, z);
        edges[1] = createXEdge(x, x2, y2, z);
        edges[2] = createXEdge(x, x2, y, z2);
        edges[3] = createXEdge(x, x2, y2, z2);

        edges[4] = createYEdge(x, y, y2, z);
        edges[5] = createYEdge(x2, y, y2, z);
        edges[6] = createYEdge(x, y, y2, z2);
        edges[7] = createYEdge(x2, y, y2, z2);

        edges[8] = createZEdge(x, y, z, z2);
        edges[9] = createZEdge(x2, y, z, z2);
        edges[10] = createZEdge(x, y2, z, z2);
        edges[11] = createZEdge(x2, y2, z, z2);

        return edges;
    }

    private static Pair<Vec3, Vec3> createXEdge(double x1, double x2, double y, double z){
        return new Pair<>(new Vec3(x1, y, z), new Vec3(x2, y, z));
    }
    private static Pair<Vec3, Vec3> createYEdge(double x, double y1, double y2, double z){
        return new Pair<>(new Vec3(x, y1, z), new Vec3(x, y2, z));
    }
    private static Pair<Vec3, Vec3> createZEdge(double x, double y, double z1, double z2){
        return new Pair<>(new Vec3(x, y, z1), new Vec3(x, y, z2));
    }

    public void updateCenter(){
        center = new Vec3(
                min.x + xLength() / 2.0,
                min.y + yLength() / 2.0,
                min.z + zLength() / 2.0
        );

        double d = BoundingBoxSettings.renderSize();
        renderBox = new AABB(
                center.x - d,
                center.y - d,
                center.z - d,
                center.x + d,
                center.y + d,
                center.z + d
        );
    }

    @Override
    public WorldSelector selector() {
        return this.selector;
    }

    @Override
    public Vec3 getPos() {
        return center;
    }

    @Override //This sets the center
    public void setPos(double x, double y, double z) {
        double _x = xLength() / 2.0;
        double _y = yLength() / 2.0;
        double _z = zLength() / 2.0;


        setValues(
                x - _x, y - _y, z - _z,
                x + _x, y + _y, z + _z
        );

        updateCenter();
    }

    public double xLength(){
        return max.x - min.x;
    }

    public double yLength(){
        return max.y - min.y;
    }

    public double zLength(){
        return max.z - min.z;
    }

    @Override
    public EditorReference reference() {
        return reference;
    }

    @Override
    public double scaleX() {
        return xLength();
    }

    @Override
    public double scaleY() {
        return yLength();
    }

    @Override
    public double scaleZ() {
        return zLength();
    }

    @Override
    public void setScale(double x, double y, double z) {
        setValues(
                center.x - x / 2.0,
                center.y - y / 2.0,
                center.z - z / 2.0,
                center.x + x / 2.0,
                center.y + y / 2.0,
                center.z + z / 2.0);
    }

    public void handleSelectorUpdate() {

    }

    private static class StretchSelector extends ScreenSelector {

        private AABB box = new AABB(0, 0, 0, 0, 0, 0);

        private void update(Vec3 vec1, Vec3 vec2){
            double x1 = vec1.x;
            double y1 = vec1.y;
            double z1 = vec1.z;
            double x2 = vec2.x;
            double y2 = vec2.y;
            double z2 = vec2.z;

            double d = 0.05;
            //Adjust so they don't stick out, yes i know its janky but idc
            if(x1 != x2) {
                x1 += d; x2 -= d;
            }else if(y1 != y2){
                y1 += d; y2 -= d;
            }else if(z1 != z2){
                z1 += d; z2 -= d;
            }

            box = new AABB(x1 - d, y1 - d, z1 - d, x2 + d, y2 + d, z2 + d);
        }

        @Override
        public AABB getBox() {
            return box;
        }
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
            if(ToolManager.get().getEditing() != EditorBoundingBox.this) return;
            if(ToolManager.get().getSelectedTool() instanceof MoveTool moveTool){
                moveTool.vectorClickCallback(EditorBoundingBox.this, press);
            }else if(ToolManager.get().getSelectedTool() instanceof ScaleTool scaleTool){
                scaleTool.vectorClickCallback(EditorBoundingBox.this, press);
            }
        }

        @Override
        public AABB getBox() {
            return renderBox;
        }
    }
}
