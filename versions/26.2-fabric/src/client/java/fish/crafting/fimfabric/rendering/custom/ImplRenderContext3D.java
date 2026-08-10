package fish.crafting.fimfabric.rendering.custom;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

//26.2
//
// Minecraft 26.x split world rendering into an "extraction" and a "drawing" phase: geometry is no
// longer written straight into a VertexConsumer, it is handed to a SubmitNodeCollector as a callback
// that runs later. FIM's RenderContext3D is imperative, so this implementation records every vertex
// while the mod draws and submits the recorded batches in endRender().
//
// Line width is a per-vertex attribute now (DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH),
// so a single line RenderType is enough - no more one layer per width.
public class ImplRenderContext3D implements RenderContext3D {

    private static final RenderPipeline PIPELINE_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("fim/pipeline/lines")
                    .withDepthStencilState(Optional.empty()) //draw through walls
                    .build());

    private static final RenderPipeline PIPELINE_BOX = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("fim/pipeline/filled_box")
                    .withDepthStencilState(Optional.empty())
                    .build());

    private static final RenderPipeline PIPELINE_STRIP = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("fim/pipeline/triangle_strip")
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
                    .withCull(false)
                    .withDepthStencilState(Optional.empty())
                    .build());

    private static final RenderType LINE_LAYER = layer("fim:lines", PIPELINE_LINES);
    private static final RenderType BOX_LAYER = layer("fim:box", PIPELINE_BOX);
    private static final RenderType STRIP_LAYER = layer("fim:strip", PIPELINE_STRIP);

    private static RenderType layer(String name, RenderPipeline pipeline) {
        return RenderType.create(name, RenderSetup.builder(pipeline)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup());
    }

    private static final int LINE_STRIDE = 10; //x y z r g b nx ny nz width
    private static final int QUAD_STRIDE = 7;  //x y z r g b a

    private final LevelRenderContext mcContext;

    /** Translation stack; RenderContext3D only ever translates, never rotates or scales. */
    private final Deque<double[]> matrixStack = new ArrayDeque<>();
    private double offsetX, offsetY, offsetZ;

    private final FloatArrayList lineVertices = new FloatArrayList();
    private final FloatArrayList boxVertices = new FloatArrayList();
    private final List<FloatArrayList> strips = new ArrayList<>();

    private float currentLineWidth = 2f;

    public ImplRenderContext3D(LevelRenderContext context) {
        this.mcContext = context;
    }

    @Override
    public void beginRender() {
        lineVertices.clear();
        boxVertices.clear();
        strips.clear();
        matrixStack.clear();
        offsetX = offsetY = offsetZ = 0;
    }

    @Override
    public void endRender() {
        if (!boxVertices.isEmpty()) {
            submitQuads(BOX_LAYER, boxVertices);
        }

        for (FloatArrayList strip : strips) {
            //Every strip has to be its own draw, otherwise consecutive strips would be stitched together
            submitQuads(STRIP_LAYER, strip);
        }

        if (!lineVertices.isEmpty()) {
            submitLines();
        }

        beginRender();
    }

    //The callback only runs in the drawing phase, long after endRender() reset the buffers,
    //so every batch has to hand over a copy of its vertices.
    private void submitQuads(RenderType layer, FloatArrayList source) {
        FloatArrayList vertices = new FloatArrayList(source);
        mcContext.submitNodeCollector().submitCustomGeometry(mcContext.poseStack(), layer, (pose, buffer) -> {
            for (int i = 0; i < vertices.size(); i += QUAD_STRIDE) {
                buffer.addVertex(pose, vertices.getFloat(i), vertices.getFloat(i + 1), vertices.getFloat(i + 2))
                        .setColor(vertices.getFloat(i + 3), vertices.getFloat(i + 4), vertices.getFloat(i + 5), vertices.getFloat(i + 6));
            }
        });
    }

    private void submitLines() {
        FloatArrayList vertices = new FloatArrayList(lineVertices);
        mcContext.submitNodeCollector().submitCustomGeometry(mcContext.poseStack(), LINE_LAYER, (pose, buffer) -> {
            for (int i = 0; i < vertices.size(); i += LINE_STRIDE) {
                buffer.addVertex(pose, vertices.getFloat(i), vertices.getFloat(i + 1), vertices.getFloat(i + 2))
                        .setColor(vertices.getFloat(i + 3), vertices.getFloat(i + 4), vertices.getFloat(i + 5), 1f)
                        .setNormal(pose, vertices.getFloat(i + 6), vertices.getFloat(i + 7), vertices.getFloat(i + 8))
                        .setLineWidth(vertices.getFloat(i + 9));
            }
        });
    }

    @Override
    public void setLineWidth(float lineWidth) {
        this.currentLineWidth = lineWidth;
    }

    @Override
    public void renderLineGradient(float x1, float y1, float z1, float x2, float y2, float z2,
                                   float r1, float g1, float b1, float r2, float g2, float b2) {
        float dX = x2 - x1;
        float dY = y2 - y1;
        float dZ = z2 - z1;

        float length = (float) Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        if (length == 0f) return;

        float nX = dX / length;
        float nY = dY / length;
        float nZ = dZ / length;

        lineVertex(x1, y1, z1, r1, g1, b1, nX, nY, nZ);
        lineVertex(x2, y2, z2, r2, g2, b2, nX, nY, nZ);
    }

    private void lineVertex(float x, float y, float z, float r, float g, float b, float nX, float nY, float nZ) {
        lineVertices.add((float) (x + offsetX));
        lineVertices.add((float) (y + offsetY));
        lineVertices.add((float) (z + offsetZ));
        lineVertices.add(r);
        lineVertices.add(g);
        lineVertices.add(b);
        lineVertices.add(nX);
        lineVertices.add(nY);
        lineVertices.add(nZ);
        lineVertices.add(currentLineWidth);
    }

    @Override
    public void renderFilledBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                                float red, float green, float blue, float alpha) {
        float x1 = (float) (minX + offsetX), y1 = (float) (minY + offsetY), z1 = (float) (minZ + offsetZ);
        float x2 = (float) (maxX + offsetX), y2 = (float) (maxY + offsetY), z2 = (float) (maxZ + offsetZ);

        //-Z
        quad(x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, red, green, blue, alpha);
        //+Z
        quad(x1, y2, z2, x2, y2, z2, x2, y1, z2, x1, y1, z2, red, green, blue, alpha);
        //-X
        quad(x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, red, green, blue, alpha);
        //+X
        quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, red, green, blue, alpha);
        //-Y
        quad(x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, red, green, blue, alpha);
        //+Y
        quad(x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, red, green, blue, alpha);
    }

    private void quad(float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz,
                      float r, float g, float b, float a) {
        boxVertex(ax, ay, az, r, g, b, a);
        boxVertex(bx, by, bz, r, g, b, a);
        boxVertex(cx, cy, cz, r, g, b, a);
        boxVertex(dx, dy, dz, r, g, b, a);
    }

    private void boxVertex(float x, float y, float z, float r, float g, float b, float a) {
        boxVertices.add(x);
        boxVertices.add(y);
        boxVertices.add(z);
        boxVertices.add(r);
        boxVertices.add(g);
        boxVertices.add(b);
        boxVertices.add(a);
    }

    @Override
    public void renderBoxOutline(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                                 float red, float green, float blue, float alpha) {
        float x1 = (float) minX, y1 = (float) minY, z1 = (float) minZ;
        float x2 = (float) maxX, y2 = (float) maxY, z2 = (float) maxZ;

        //bottom
        renderLine(x1, y1, z1, x2, y1, z1, red, green, blue);
        renderLine(x2, y1, z1, x2, y1, z2, red, green, blue);
        renderLine(x2, y1, z2, x1, y1, z2, red, green, blue);
        renderLine(x1, y1, z2, x1, y1, z1, red, green, blue);

        //top
        renderLine(x1, y2, z1, x2, y2, z1, red, green, blue);
        renderLine(x2, y2, z1, x2, y2, z2, red, green, blue);
        renderLine(x2, y2, z2, x1, y2, z2, red, green, blue);
        renderLine(x1, y2, z2, x1, y2, z1, red, green, blue);

        //verticals
        renderLine(x1, y1, z1, x1, y2, z1, red, green, blue);
        renderLine(x2, y1, z1, x2, y2, z1, red, green, blue);
        renderLine(x2, y1, z2, x2, y2, z2, red, green, blue);
        renderLine(x1, y1, z2, x1, y2, z2, red, green, blue);
    }

    @Override
    public Vec3 camera() {
        return mcContext.levelState().cameraRenderState.pos;
    }

    @Override
    public void translate(double x, double y, double z) {
        offsetX += x;
        offsetY += y;
        offsetZ += z;
    }

    @Override
    public void push() {
        matrixStack.push(new double[]{offsetX, offsetY, offsetZ});
    }

    @Override
    public void pop() {
        double[] previous = matrixStack.pop();
        offsetX = previous[0];
        offsetY = previous[1];
        offsetZ = previous[2];
    }

    @Override
    public float tickDelta() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
    }

    @Override
    public VertexHelper renderVertices() {
        FloatArrayList strip = new FloatArrayList();
        strips.add(strip);

        return (x, y, z, color) -> {
            strip.add((float) (x + offsetX));
            strip.add((float) (y + offsetY));
            strip.add((float) (z + offsetZ));
            strip.add(((color >> 16) & 0xFF) / 255f);
            strip.add(((color >> 8) & 0xFF) / 255f);
            strip.add((color & 0xFF) / 255f);
            strip.add(((color >>> 24) & 0xFF) / 255f);
        };
    }
}
