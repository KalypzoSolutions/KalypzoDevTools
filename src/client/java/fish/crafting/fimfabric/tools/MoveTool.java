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
import fish.crafting.fimfabric.tools.snapping.PositionSnapping;
import fish.crafting.fimfabric.tools.selector.WorldSelector;
import fish.crafting.fimfabric.tools.selector.WorldSelectorManager;
import fish.crafting.fimfabric.ui.TexRegistry;
import fish.crafting.fimfabric.util.*;
import fish.crafting.fimfabric.util.render.FadeTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static fish.crafting.fimfabric.tools.snapping.PositionSnapping.ALL_SNAPPINGS;

public class MoveTool extends CustomTool<Positioned> {

    private static int SNAPPING_INDEX = 1;
    public static PositionSnapping SNAPPING = ALL_SNAPPINGS[SNAPPING_INDEX];

    static {
        SNAPPING.fade.fadeIn();
    }

    private final BoundingBox[] axisBoundingBoxes;
    private final List<WorldSelector> axisSelectors = new ArrayList<>();

    private final double handlePartLength = 0.4;
    private final double handlePartWidth = 0.008;
    private final double offsetFromCenter = 0.5;
    private final double headPartWidth = 0.05;
    private final double headPartLength = 0.15;

    private boolean editingPos = false;
    private double editingDistance = 0d;
    //Offset from the center of the vector, used so that axis-selecting won't snap
    private Double editingCoordOffset = null;
    private ToolAxis editingAxis = null;
    private final FadeTracker fadingText = new FadeTracker(0, 0.3, 0.1);
    private static final FadeTracker snapTextFade = new FadeTracker(0, 0.1, 0.3);
    private Vec3 startPos = null;
    private Positioned lastRendered = null;

    public MoveTool(){
        ToolAxis[] values = ToolAxis.values();
        axisBoundingBoxes = new BoundingBox[values.length];
        for (int i = 0; i < values.length; i++) {
            axisBoundingBoxes[i] = new BoundingBox();
            axisSelectors.add(new Selector(axisBoundingBoxes[i], values[i]));
        }
    }

    @Override
    public void onEnable() {
        stopEditing();
        this.startPos = null;
    }

    private void stopEditing(){
        editingPos = false;
        editingAxis = null;
    }

    private void startEditingPosDirectly(@NotNull Positioned positioned, Vec3 camera){
        this.editingPos = true;
        this.editingDistance = camera.distanceTo(positioned.getPos());
        this.editingAxis = null;
        fadingText.begin();
    }

    private void startEditingPosAxis(@NotNull ToolAxis axis){
        this.editingPos = true;
        this.editingAxis = axis;
        this.editingCoordOffset = null;
    }

    public void vectorClickCallback(Positioned positioned, boolean click){
        if(click) {
            Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
            startEditingPosDirectly(positioned, camera);
        }else{
            stopEditing();
        }
    }

    @Override
    public void render2D(ScreenRenderContext context, DeltaTracker counter) {
        Font textRenderer = Minecraft.getInstance().font;

        int windowWidth = WindowUtil.scaledWidth();
        int snappingWidth = 90;
        int windowHeight = WindowUtil.scaledHeight();
        int snappingHeight = 50;

        int snappingX = windowWidth - snappingWidth - 10;
        int snappingY = windowHeight - snappingHeight - 20;

        int fontHeight = context.fontHeight();

        context.drawCenteredTextWithShadow("Alt + Scroll to",
                snappingX + snappingWidth / 2,
                snappingY - 32 - fontHeight,
                0x99FFFFFF);

        context.drawCenteredTextWithShadow("change snapping",
                snappingX + snappingWidth / 2,
                snappingY - 30,
                0x99FFFFFF);

        context.drawGradientBox(
                snappingX,
                snappingY,
                snappingWidth,
                snappingHeight,
                15,
                0xAA000000,
                true
        );

        int i = 0;
        for (PositionSnapping snapping : ALL_SNAPPINGS) {
            int alpha = 128 + snapping.fade.alpha() / 2;
            int color = ColorUtil.alpha(0xFFFFFFFF, alpha);

            context.drawText(
                    snapping.getName(),
                    snappingX + 3,
                    (int) (snappingY + fontHeight * 1.5 * i++ + 3),
                    color,
                    true
            );
        }

        if(fadingText.isActive()) {
            int clr = fadingText.color(0xFFFFFFFF);
            context.drawCenteredTextWithShadow(
                    "Distance to Object: " + NumUtil.betterNumber(editingDistance),
                    WindowUtil.scaledWidth() / 2,
                    windowHeight * 3 / 4,
                    clr
            );
        }

        if(snapTextFade.isActive()){
            context.drawCenteredTextWithShadow(
                    "Snapping: " + SNAPPING.getName(),
                    WindowUtil.scaledWidth() / 2,
                    WindowUtil.scaledHeight() / 2 - 30,
                    snapTextFade.color(0xFFFFFFFF)
            );
        }

        if(lastRendered != null && startPos != null) {
            context.drawCenteredTextWithShadow(
                    "Before: " + stringify(startPos),
                    WindowUtil.scaledWidth() / 2,
                    40,
                    0xEE89FF93
            );

            context.drawCenteredTextWithShadow(
                    "After: " + stringify(lastRendered.getPos()),
                    WindowUtil.scaledWidth() / 2,
                    (int) (40 + textRenderer.lineHeight * 1.5),
                    0xEE89C2FF
            );

            context.drawCenteredTextWithShadow(
                    "Press ENTER to confirm edit.",
                    WindowUtil.scaledWidth() / 2,
                    (int) (40 + textRenderer.lineHeight * 3),
                    0xEE89FF93
            );

            if(KeyUtil.isShiftPressed()){
                context.drawCenteredTextWithShadow(
                        "Won't clear and return to IntelliJ",
                        WindowUtil.scaledWidth() / 2,
                        (int) (40 + textRenderer.lineHeight * 4.5),
                        0xAAFF0000
                );
            }
        }
    }

    private String stringify(Vec3 pos){
        return "[" + NumUtil.betterNumber(pos.x) + ", " + NumUtil.betterNumber(pos.y) + ", " + NumUtil.betterNumber(pos.z) + "]";
    }

    @Override
    protected void render(RenderContext3D context, Positioned obj) {
        this.lastRendered = obj;
        if(this.startPos == null) {
            this.startPos = obj.getPos();
        }

        context.push();
        context.translateCamera();

        if(this.editingPos) {
            Vec3 pos = obj.getPos();
            int x = Mth.floor(pos.x);
            int y = Mth.floor(pos.y);
            int z = Mth.floor(pos.z);

            context.renderFilledBox(
                    x, y, z,
                    x + 1, y + 1, z + 1,
                    1f, 0.8f, 0.8f, 0.5f
            );

            context.renderBoxOutline(
                    x, y, z,
                    x + 1, y + 1, z + 1,
                    1f, 0.8f, 0.8f, 0.4f
            );
        }

        context.pop();

        Vec3 camera = context.camera();

        if(Minecraft.getInstance().player != null && editingPos){
            Vec3 rot = Minecraft.getInstance().player.getViewVector(context.tickDelta());

            if(editingAxis != null){
                Double coord = getPlaneIntersectionCoordinate(
                        obj,
                        editingAxis,
                        camera,
                        rot);

                if(coord != null){
                    if(this.editingCoordOffset == null){
                        this.editingCoordOffset = coord - VectorUtils.getValueUsingUnitVector(obj.getPos(), editingAxis.unit);
                    }

                    coord -= this.editingCoordOffset;

                    double x = editingAxis == ToolAxis.X ? coord : obj.getPos().x;
                    double y = editingAxis == ToolAxis.Y ? coord : obj.getPos().y;
                    double z = editingAxis == ToolAxis.Z ? coord : obj.getPos().z;

                    x = SNAPPING.snapXZ(x);
                    y = SNAPPING.snapY(y);
                    z = SNAPPING.snapXZ(z);

                    obj.setPos(x, y, z);
                }
            }else{
                Vec3 newPos = camera.add(rot.scale(editingDistance));

                double x = SNAPPING.snapXZ(newPos.x);
                double y = SNAPPING.snapY(newPos.y);
                double z = SNAPPING.snapXZ(newPos.z);

                obj.setPos(x, y, z);
            }
        }

        double zoom = calculateToolZoom(camera, obj);

        WorldSelector mainActiveSelector = WorldSelectorManager.get().getMainActiveSelector();
        ToolAxis hoveredAxis = null;
        if(mainActiveSelector instanceof Selector selector) {
            hoveredAxis = selector.axis;
        }

        for (ToolAxis value : ToolAxis.values()) {
            renderHandle(context, value, zoom, obj.getPos(), value.color(hoveredAxis));
            renderHead(context, value, zoom, obj.getPos(), value.color(hoveredAxis));
        }

        for (ToolAxis value : ToolAxis.values()) {
            Vec3 u = value.unit.scale(zoom * (handlePartLength + headPartLength));

            Vec3 origin = obj.getPos().add(value.unit.scale((offsetFromCenter - handlePartLength) * zoom));
            Vec3 a = value.oppositeUnit.scale(headPartWidth * 0.8 * zoom);

            BoundingBox box = axisBoundingBoxes[value.ordinal()];
            box.change(
                    origin.x + a.x,
                    origin.y + a.y,
                    origin.z + a.z,
                    origin.x - a.x + u.x,
                    origin.y - a.y + u.y,
                    origin.z - a.z + u.z);

        }
    }

    @Override
    public boolean isAccessibleFor(Object object) {
        return object instanceof Positioned;
    }

    @Override
    public Identifier getTexture() {
        return TexRegistry.TOOL_MOVE;
    }

    private void renderHandle(RenderContext3D context,
                              ToolAxis facing,
                              double zoom,
                              Vec3 position,
                              int color){
        context.push();
        context.translateCamera();

        RenderContext3D.VertexHelper vertexHelper = context.renderVertices();

        int angles = 9;
        double rotate = Math.TAU / (angles);
        Vec3 unit = facing.unit;

        double x = unit.x * handlePartWidth * zoom;
        double y = unit.y * handlePartWidth * zoom;
        double z = unit.z * handlePartWidth * zoom;

        double xLen = unit.x * handlePartLength * zoom;
        double yLen = unit.y * handlePartLength * zoom;
        double zLen = unit.z * handlePartLength * zoom;

        double anchorX = position.x + unit.x * (offsetFromCenter - handlePartLength) * zoom;
        double anchorY = position.y + unit.y * (offsetFromCenter - handlePartLength) * zoom;
        double anchorZ = position.z + unit.z * (offsetFromCenter - handlePartLength) * zoom;

        for (int i = 0; i < angles; i++) {
            double ang1 = rotate * i;
            double ang2 = rotate * (i + 1);

            double sin1 = Math.sin(ang1);
            double sin2 = Math.sin(ang2);

            double cos1 = Math.cos(ang1);
            double cos2 = Math.cos(ang2);

            vertexHelper.vertex((float) (anchorX + y * sin2 + z * sin2 + xLen),
                            (float) (anchorY + x * sin2 + z * cos2 + yLen),
                            (float) (anchorZ + x * cos2 + y * cos2 + zLen), color);

            vertexHelper.vertex((float) (anchorX + y * sin1 + z * sin1),
                            (float) (anchorY + x * sin1 + z * cos1),
                            (float) (anchorZ + x * cos1 + y * cos1), color);

            vertexHelper.vertex((float) (anchorX + y * sin2 + z * sin2),
                            (float) (anchorY + x * sin2 + z * cos2),
                            (float) (anchorZ + x * cos2 + y * cos2), color);

            vertexHelper.vertex((float) (anchorX + y * sin1 + z * sin1 + xLen),
                            (float) (anchorY + x * sin1 + z * cos1 + yLen),
                            (float) (anchorZ + x * cos1 + y * cos1 + zLen), color);
        }

        context.pop();
    }

    private void renderHead(RenderContext3D context,
                            ToolAxis facing,
                            double zoom,
                            Vec3 position,
                            int color){
        context.push();
        context.translateCamera();

        RenderContext3D.VertexHelper vertexHelper = context.renderVertices();

        int angles = 36;
        double rotate = Math.TAU / (angles);
        Vec3 unit = facing.unit;

        double x = unit.x * headPartWidth * zoom;
        double y = unit.y * headPartWidth * zoom;
        double z = unit.z * headPartWidth * zoom;

        double anchorX = position.x + unit.x * offsetFromCenter * zoom;
        double anchorY = position.y + unit.y * offsetFromCenter * zoom;
        double anchorZ = position.z + unit.z * offsetFromCenter * zoom;

        //sin0 = 0, cos0 = 1
        Vec3 firstAnglePos = new Vec3(
                0, z, y
        );

        //Render base
        for (int i = 1; i < angles; i++) {
            double ang1 = rotate * i;
            double ang2 = rotate * (i + 1);

            double sin1 = Math.sin(ang1);
            double sin2 = Math.sin(ang2);

            double cos1 = Math.cos(ang1);
            double cos2 = Math.cos(ang2);

            vertexHelper.vertex( (float) (anchorX + firstAnglePos.x),
                            (float) (anchorY + firstAnglePos.y),
                            (float) (anchorZ + firstAnglePos.z), color);

            vertexHelper.vertex( (float) (anchorX + y * sin2 + z * sin2),
                            (float) (anchorY + x * sin2 + z * cos2),
                            (float) (anchorZ + x * cos2 + y * cos2), color);

            vertexHelper.vertex( (float) (anchorX + y * sin1 + z * sin1),
                            (float) (anchorY + x * sin1 + z * cos1),
                            (float) (anchorZ + x * cos1 + y * cos1), color);
        }

        for (int i = 0; i < angles; i++) {
            double ang1 = rotate * i;
            double ang2 = rotate * (i + 1);

            double sin1 = Math.sin(ang1);
            double sin2 = Math.sin(ang2);

            double cos1 = Math.cos(ang1);
            double cos2 = Math.cos(ang2);

            if(facing == ToolAxis.Z) {
                vertexHelper.vertex((float) (anchorX + unit.x * headPartLength * zoom),
                                (float) (anchorY + unit.y * headPartLength * zoom),
                                (float) (anchorZ + unit.z * headPartLength * zoom), color);

                vertexHelper.vertex((float) (anchorX + y * sin2 + z * sin2),
                                (float) (anchorY + x * sin2 + z * cos2),
                                (float) (anchorZ + x * cos2 + y * cos2), color);

                vertexHelper.vertex((float) (anchorX + y * sin1 + z * sin1),
                                (float) (anchorY + x * sin1 + z * cos1),
                                (float) (anchorZ + x * cos1 + y * cos1), color);
            }else if(facing == ToolAxis.Y){
                vertexHelper.vertex((float) (anchorX + y * sin1 + z * sin1),
                                (float) (anchorY + x * sin1 + z * cos1),
                                (float) (anchorZ + x * cos1 + y * cos1), color);

                vertexHelper.vertex((float) (anchorX + y * sin2 + z * sin2),
                                (float) (anchorY + x * sin2 + z * cos2),
                                (float) (anchorZ + x * cos2 + y * cos2), color);

                vertexHelper.vertex((float) (anchorX + unit.x * headPartLength * zoom),
                                (float) (anchorY + unit.y * headPartLength * zoom),
                                (float) (anchorZ + unit.z * headPartLength * zoom), color);
            }else{
                vertexHelper.vertex(
                                (float) (anchorX + unit.x * headPartLength * zoom),
                                (float) (anchorY + unit.y * headPartLength * zoom),
                                (float) (anchorZ + unit.z * headPartLength * zoom), color);
                vertexHelper.vertex(
                                (float) (anchorX + y * sin1 + z * sin1),
                                (float) (anchorY + x * sin1 + z * cos1),
                                (float) (anchorZ + x * cos1 + y * cos1), color);

                vertexHelper.vertex(
                                (float) (anchorX + y * sin2 + z * sin2),
                                (float) (anchorY + x * sin2 + z * cos2),
                                (float) (anchorZ + x * cos2 + y * cos2), color);

            }

        }

        context.pop();
    }

    /**
     * When moving using the axis-arrows, the axis creates a big ass plane that we need to find the intersection
     * of using the player's view direction, and extract the wanted coordinate position :D
     */
    private Double getPlaneIntersectionCoordinate(Positioned positioned,
                                                  ToolAxis planeFacing,
                                                  Vec3 camera,
                                                  Vec3 facing){
        Vec3 vec3d = VectorUtils.lineIntersection(
                positioned.getPos(),
                facing.multiply(planeFacing.oppositeUnit).normalize(),
                camera,
                facing);

        if(vec3d != null){
            return VectorUtils.sumCoords(vec3d.multiply(planeFacing.unit));
        }
        //for (Vec3d normal : planeFacing.planeNormals) {
        //}

        return null;
    }

    @Override
    public boolean onScroll(double scroll) {
        if(KeyUtil.isAltPressed()){
            moveSnapping((int) -scroll);

            return true;
        }

        if(!editingPos || editingAxis != null) return false; //Is editing vector directly

        double move = 0.5 * scroll;
        if(KeyUtil.isControlPressed()) move *= 4;
        if(KeyUtil.isShiftPressed()) move *= 0.5;

        editingDistance += move;
        if(editingDistance < 0) editingDistance = 0;

        fadingText.begin();
        return true;
    }

    public static void moveSnapping(int by) {
        int before = SNAPPING_INDEX;

        SNAPPING_INDEX += by;

        if(SNAPPING_INDEX < 0) SNAPPING_INDEX = 0;
        else if(SNAPPING_INDEX >= ALL_SNAPPINGS.length) SNAPPING_INDEX = ALL_SNAPPINGS.length - 1;

        if(before == SNAPPING_INDEX) return; //Nothing changed

        SoundUtil.play(SoundManager.SWITCH, 0.6f + SNAPPING_INDEX * 0.05f);

        SNAPPING.fade.fadeOut();
        SNAPPING = ALL_SNAPPINGS[SNAPPING_INDEX];
        SNAPPING.fade.fadeIn();

        snapTextFade.begin();
    }

    @Override
    protected void confirmEdit(Positioned positioned) {
        switch (positioned) {
            case EditorVector vector -> new F2IDoVectorEditPacket(vector).send();
            case EditorLocation location -> new F2IDoLocationEditPacket(location).send();
            case EditorBoundingBox boundingBox -> new F2IDoBoundingBoxEditPacket(boundingBox).send();
            default -> {}
        }

        startPos = positioned.getPos();
    }

    @Override
    public List<WorldSelector> getSelectors() {
        return axisSelectors;
    }

    public class Selector extends WorldSelector {

        private final BoundingBox boundingBox;
        private final ToolAxis axis;

        private Selector(BoundingBox box, ToolAxis axis){
            this.boundingBox = box;
            this.axis = axis;
        }

        @Override
        protected void onPress(int button, int mods) {
            startEditingPosAxis(axis);
        }

        @Override
        protected void onUnPress() {
            stopEditing();
        }

        @Override
        public AABB getBox() {
            return boundingBox.toMCBox();
        }
    }
}
