package fish.crafting.fimfabric.tools;

import fish.crafting.fimfabric.connection.packets.F2IDoBoundingBoxEditPacket;
import fish.crafting.fimfabric.connection.packets.F2IDoLocationEditPacket;
import fish.crafting.fimfabric.connection.packets.F2IDoVectorEditPacket;
import fish.crafting.fimfabric.editor.values.EditorBoundingBox;
import fish.crafting.fimfabric.editor.values.EditorLocation;
import fish.crafting.fimfabric.editor.values.EditorVector;
import fish.crafting.fimfabric.rendering.custom.RenderContext3D;
import fish.crafting.fimfabric.rendering.custom.ScreenRenderContext;
import fish.crafting.fimfabric.tools.render.ToolAxis;
import fish.crafting.fimfabric.tools.selector.CircularSelector;
import fish.crafting.fimfabric.tools.selector.WorldSelector;
import fish.crafting.fimfabric.tools.selector.WorldSelectorManager;
import fish.crafting.fimfabric.ui.TexRegistry;
import fish.crafting.fimfabric.util.*;
import fish.crafting.fimfabric.util.render.FadeTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import fish.crafting.fimfabric.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RotateTool extends CustomTool<PosRotated> {

    private static final ToolAxis[] ROT_AXIS = {ToolAxis.X, ToolAxis.Y};
    private final BoundingBox[] axisBoundingBoxes;
    private final List<WorldSelector> axisSelectors = new ArrayList<>();

    private final double distanceFromCenter = 1.0;
    private final double lineWidth = 0.01;

    private boolean editing = false;

    private ToolAxis editingAxis = null;
    private Pair<Float, Float> startRot = null;
    private Vec3d startRotDir = null;
    private PosRotated lastRendered = null;

    private final FadeTracker fadingText = new FadeTracker(0, 0.3, 0.1);

    public RotateTool(){
        axisBoundingBoxes = new BoundingBox[ROT_AXIS.length];
        for (int i = 0; i < ROT_AXIS.length; i++) {
            axisBoundingBoxes[i] = new BoundingBox();
            axisSelectors.add(new Selector(axisBoundingBoxes[i], ROT_AXIS[i]));
        }
    }

    @Override
    public void onEnable() {
        stopEditing();
        startRot = null;
    }

    private void stopEditing(){
        editing = false;
        editingAxis = null;
        startRotDir = null;
    }

    private void startEditingDirectly(){
        editing = true;
        editingAxis = null;
        fadingText.begin();
    }

    private void startEditingAxis(@NotNull ToolAxis axis){
        if(Arrays.stream(ROT_AXIS).noneMatch(a -> a == axis)) return;

        editing = true;
        editingAxis = axis;
    }

    public void locationClickCallback(PosRotated positioned, boolean click){
        if(click) {
            startEditingDirectly();
        }else{
            stopEditing();
        }
    }

    @Override
    public void render2D(ScreenRenderContext context, RenderTickCounter counter) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        if(fadingText.isActive()) {
            int clr = fadingText.color(0xFFFFFFFF);
            /*context.drawCenteredTextWithShadow(
                    textRenderer,
                    "Distance to Object: " + NumUtil.betterNumber(editingDistance),
                    WindowUtil.scaledWidth() / 2,
                    WindowUtil.scaledHeight() * 3 / 4,
                    clr
            );*/
        }

        if(lastRendered != null && startRot != null) {
            context.drawCenteredTextWithShadow(
                    "Before: " + stringify(startRot),
                    WindowUtil.scaledWidth() / 2,
                    40,
                    0xEE89FF93
            );

            context.drawCenteredTextWithShadow(
                    "After: " + stringify(lastRendered.pitch(), lastRendered.yaw()),
                    WindowUtil.scaledWidth() / 2,
                    (int) (40 + textRenderer.fontHeight * 1.5),
                    0xEE89C2FF
            );

            context.drawCenteredTextWithShadow(
                    "Press ENTER to confirm edit.",
                    WindowUtil.scaledWidth() / 2,
                    (int) (40 + textRenderer.fontHeight * 3),
                    0xEE89FF93
            );

            if(KeyUtil.isShiftPressed()){
                context.drawCenteredTextWithShadow(
                        "Won't clear and return to IntelliJ",
                        WindowUtil.scaledWidth() / 2,
                        (int) (40 + textRenderer.fontHeight * 4.5),
                        0xAAFF0000
                );
            }
        }
    }

    private String stringify(Pair<Float, Float> rot){
        return stringify(rot.getLeft(), rot.getRight());
    }

    private String stringify(float pitch, float yaw){
        return "Pitch: " + NumUtil.betterNumber(pitch) + " Yaw: " + NumUtil.betterNumber(yaw);
    }

    @Override
    protected void render(RenderContext3D context, PosRotated obj) {
        this.lastRendered = obj;
        if(this.startRot == null) {
            this.startRot = new Pair<>(obj.pitch(), obj.yaw());
        }

        if(this.startRotDir == null){
            this.startRotDir = VectorUtils.getDirection(obj.pitch(), obj.yaw());
        }

        Vec3d camera = context.camera();

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if(player != null && editing){
            Vec3d rot = player.getRotationVecClient();

            if(editingAxis != null){
                Vec3d vec3d = getPlaneIntersectionPoint(
                        obj,
                        editingAxis,
                        camera,
                        rot);

                if(vec3d != null && startRotDir != null){
                    Vec3d dir = vec3d.subtract(obj.getPos()).normalize();

                    if(editingAxis == ToolAxis.Y) {
                        obj.setRotation(obj.pitch(), VectorUtils.yawFromDirection(dir));
                    }else{
                        float pitch = VectorUtils.pitchFromDirection(dir);
                        if(pitch < -90f || pitch > 180f) pitch = -90f;
                        else if(pitch > 90f) pitch = 90f;

                        obj.setRotation(pitch, obj.yaw());
                    }
                }
            }else{
                obj.setRotation(player.getPitch(), player.getYaw());
            }
        }


        double zoom = calculateToolZoom(camera, obj);

        WorldSelector mainActiveSelector = WorldSelectorManager.get().getMainActiveSelector();
        ToolAxis hoveredAxis = null;
        if(mainActiveSelector instanceof Selector selector) {
            hoveredAxis = selector.axis;
        }

        for (ToolAxis value : ROT_AXIS) {
            float rotateDeg;
            if(value == ToolAxis.Y){
                rotateDeg = (float) Math.toRadians(-obj.yaw());
            }else {
                rotateDeg = (float) Math.toRadians(-obj.pitch());
            }

            renderAxis(context, value, zoom, obj.getPos(), value.color(hoveredAxis), rotateDeg);
        }

        Vec3d pos = obj.getPos();
        if(editing && startRotDir != null){
            context.setLineWidth(8f);

            context.push();
            context.translateCamera();

            context.renderLine(
                    (float) pos.x,
                    (float) pos.y,
                    (float) pos.z,
                    (float) (pos.x + startRotDir.x),
                    (float) (pos.y + startRotDir.y),
                    (float) (pos.z + startRotDir.z),
                    1f, 1f, 1f
            );

            context.pop();
        }

        for (ToolAxis value : ROT_AXIS) {

            double x, y, z;
            x = y = z = (lineWidth + distanceFromCenter) * zoom + 0.02;

            double small = lineWidth * zoom;
            if(value == ToolAxis.X) x = small;
            if(value == ToolAxis.Y) y = small;
            if(value == ToolAxis.Z) z = small;

            BoundingBox box = axisBoundingBoxes[value.ordinal()];
            box.change(
                    pos.x + x,
                    pos.y + y,
                    pos.z + z,
                    pos.x - x,
                    pos.y - y,
                    pos.z - z);

        }
    }

    @Override
    public boolean isAccessibleFor(Object object) {
        return object instanceof PosRotated;
    }

    @Override
    public Identifier getTexture() {
        return TexRegistry.TOOL_ROTATE;
    }

    private void renderAxis(RenderContext3D context,
                            ToolAxis facing,
                            double zoom,
                            Vec3d position,
                            int color,
                            float rotateDeg){
        context.push();
        context.translateCamera();

        RenderContext3D.VertexHelper vertexHelper = context.renderVertices();

        int bigCircleAngles = 60;
        double bigRotate = Math.TAU / bigCircleAngles;

        int smallCircleAngles = 4;
        double smallRotate = Math.TAU / smallCircleAngles;

        Vec3d unit = facing.unit;

        double zoomedDistanceFromCenter = distanceFromCenter * zoom;

        for (int i = 0; i < bigCircleAngles; i++) {

            double bigAng = bigRotate * i + rotateDeg;
            double bigAng2 = bigRotate * (i + 1) + rotateDeg;

            double bigSin1 = Math.sin(bigAng);
            double bigCos1 = Math.cos(bigAng);

            double bigSin2 = Math.sin(bigAng2);
            double bigCos2 = Math.cos(bigAng2);

            Vec3d point  = facing.makePlaneFromNormal(bigSin1, bigCos1);
            Vec3d point2 = facing.makePlaneFromNormal(bigSin2, bigCos2);

            double zoomedLineWidth = lineWidth * zoom;
            double extrudingPartZoom = 4.0;

            for (int o = 0; o < smallCircleAngles; o++) {
                double ang = smallRotate * o;
                double ang2 = smallRotate * (o + 1);

                double zoom1 = zoomedLineWidth;
                double zoom2 = zoomedLineWidth;

                //Yes this is hardcoded
                if(i == 0) {
                    zoom1 *= extrudingPartZoom;
                    zoom2 *= extrudingPartZoom / 2.0;
                }

                if(i == 1) zoom1 *= extrudingPartZoom / 2.0;

                if(i == bigCircleAngles - 1) {
                    zoom1 *= extrudingPartZoom / 2.0;
                    zoom2 *= extrudingPartZoom;
                }

                if(i == bigCircleAngles - 2) zoom2 *= extrudingPartZoom / 2.0;

                Vec3d pos1 = point
                        .multiply(Math.sin(ang) * zoom1 + zoomedDistanceFromCenter)
                        .add(unit.multiply(Math.cos(ang) * zoom1));

                Vec3d pos2 = point
                        .multiply(Math.sin(ang2) * zoom1 + zoomedDistanceFromCenter)
                        .add(unit.multiply(Math.cos(ang2) * zoom1));

                Vec3d pos3 = point2
                        .multiply(Math.sin(ang) * zoom2 + zoomedDistanceFromCenter)
                        .add(unit.multiply(Math.cos(ang) * zoom2));

                Vec3d pos4 = point2
                        .multiply(Math.sin(ang2) * zoom2 + zoomedDistanceFromCenter)
                        .add(unit.multiply(Math.cos(ang2) * zoom2));

                if(facing != ToolAxis.Y){
                    Vec3d t = pos2;
                    pos2 = pos3;
                    pos3 = t;
                }

                vertexHelper.vertex((float) (pos1.x + position.x),
                                (float) (pos1.y + position.y),
                                (float) (pos1.z + position.z), color);

                vertexHelper.vertex((float) (pos2.x + position.x),
                                (float) (pos2.y + position.y),
                                (float) (pos2.z + position.z), color);

                vertexHelper.vertex((float) (pos3.x + position.x),
                                (float) (pos3.y + position.y),
                                (float) (pos3.z + position.z), color);

                vertexHelper.vertex((float) (pos4.x + position.x),
                                (float) (pos4.y + position.y),
                                (float) (pos4.z + position.z), color);

            }
        }

        context.pop();
    }

    private Vec3d getPlaneIntersectionPoint(Positioned positioned,
                                                  ToolAxis planeFacing,
                                                  Vec3d camera,
                                                  Vec3d facing){
        return VectorUtils.lineIntersection(
                positioned.getPos(),
                planeFacing.unit,
                camera,
                facing);
    }

    @Override
    public boolean onScroll(double scroll) {
        if(!editing || editingAxis == null || lastRendered == null) return false; //Isn't editing location directly

        float move = (float) (scroll * 5f);
        if(KeyUtil.isControlPressed()) move *= 4f;
        if(KeyUtil.isShiftPressed()) move *= 0.5f;

        if(isYaw(editingAxis)){
            lastRendered.setRotation(
                    lastRendered.pitch(),
                    lastRendered.yaw() + move
            );
        }else{
            lastRendered.setRotation(
                    lastRendered.pitch() + move,
                    lastRendered.yaw()
            );
        }

        return true;
    }

    private boolean isYaw(@NotNull ToolAxis axis){
        return axis == ToolAxis.Y;
    }

    @Override
    protected void confirmEdit(PosRotated positioned) {
        switch (positioned) {
            case EditorVector vector -> new F2IDoVectorEditPacket(vector).send();
            case EditorLocation location -> new F2IDoLocationEditPacket(location).send();
            case EditorBoundingBox boundingBox -> new F2IDoBoundingBoxEditPacket(boundingBox).send();
            default -> {}
        }

        startRot = new Pair<>(positioned.pitch(), positioned.yaw());
        startRotDir = VectorUtils.getDirection(positioned.pitch(), positioned.yaw());
    }

    @Override
    public List<WorldSelector> getSelectors() {
        return axisSelectors;
    }

    public class Selector extends CircularSelector {

        private final BoundingBox boundingBox;
        private final ToolAxis axis;

        private Selector(BoundingBox box, ToolAxis axis){
            super(axis.unit, distanceFromCenter / (distanceFromCenter + lineWidth) * 0.9);
            this.boundingBox = box;
            this.axis = axis;
            this.maxRadiusExtend = lineWidth;
        }

        @Override
        protected void onPress(int button, int mods) {
            startEditingAxis(axis);
        }

        @Override
        protected void onUnPress() {
            stopEditing();
        }

        @Override
        public Box getBox() {
            return boundingBox.toMCBox();
        }
    }
}
