package fish.crafting.fimfabric.util;

import fish.crafting.fimfabric.rendering.custom.RenderContext3D;
import fish.crafting.fimfabric.util.cache.RenderFrameCache;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.Camera;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class CursorPicking {

    private static final RenderFrameCache<BlockHitResult> cachedRaycast = new RenderFrameCache<>(null);

    private static Vec3d vec1 = new Vec3d(0, 300, 0);
    private static double x = 0, y = -1, z = 0;
    public static Matrix4f projectionMatrix = new Matrix4f(), viewMatrix = new Matrix4f(), positionMatrix = new Matrix4f();

    public static void update(){
        MinecraftClient client = MinecraftClient.getInstance();
        if(client.world == null) return;

        Camera camera = client.gameRenderer.getCamera();

        //System.out.println(viewMatrix);

        double[] converted = convert(viewMatrix, projectionMatrix, MouseUtil.x(), MouseUtil.y());
        x = -converted[0];
        y = -converted[1];
        z = -converted[2];

        vec1 = camera.getPos();
    }

    public static void renderPickedPos(@NotNull RenderContext3D context,
                                       BlockPos pos, float r, float g, float b, float a){
        renderPickedPos(
                context,
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                r, g, b, a);
    }

    public static void renderPickedPos(@NotNull RenderContext3D context,
                                       double x, double y, double z,
                                       double x2, double y2, double z2, float r, float g, float b, float a){
        context.push();
        context.translateCamera();
        context.setLineWidth(2f);

        //To prevent z-fighting, we enlarge the box very slightly.
        double offset = 0.001;

        double dX = Math.copySign(offset, x2 - x);
        double dY = Math.copySign(offset, y2 - y);
        double dZ = Math.copySign(offset, z2 - z);

        x -= dX; y -= dY; z -= dZ;
        x2 += dX; y2 += dY; z += dZ;

        context.renderBoxOutline(
                x, y, z,
                x2, y2, z2,
                r, g, b, a
        );

        context.renderFilledBox(
                x, y, z,
                x2, y2, z2,
                r, g, b, 0.5f * a
        );

        context.pop();
    }

    public static boolean areBlockPickingPrerequisitesMet(){
        MinecraftClient client = MinecraftClient.getInstance();
        if(client.player == null) return false;
        if(client.player.isSleeping()) return false;

        return KeyUtil.isControlPressed() &&
                !client.mouse.isCursorLocked() &&
                McCompat.currentScreen() instanceof ChatScreen;
    }

    public static BlockHitResult raycast(){
        return raycast(KeyUtil.isShiftPressed(), 100);
    }

    public static BlockHitResult raycast(boolean allowAllBlocks, double dist) {
        return cachedRaycast.computeIfAbsent(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if(client.world == null) return null;

            ShapeContext context = (client.player == null ? ShapeContext.absent() : ShapeContext.of(client.player));

            Vec3d vec2 = createEndPoint(dist);

            var fluidHandling = RaycastContext.FluidHandling.NONE;
            var shapeType = RaycastContext.ShapeType.COLLIDER;

            if(allowAllBlocks) {
                fluidHandling = RaycastContext.FluidHandling.ANY;
                shapeType = RaycastContext.ShapeType.OUTLINE;
            }


            return client.world.raycast(new RaycastContext(vec1, vec2, shapeType, fluidHandling, context));
        });
    }

    public static Vec3d createEndPoint(double dist){
        return vec1.add(x * dist, y * dist, z * dist);
    }

    /**
        Transforms a 2D coordinate (in window space) to a 3D vector.
        Returns an array of the following format:
        [dx, dy, dz, ox, oy, oz]
        where [dx, dy, dz] is the direction vector and [ox, oy, oz] is the origin of the ray.
        Works for both orthographic and perspective projections.
        Script created by TheSnidr
    */
    private static double[] convert(Matrix4f V, Matrix4f P, double _x, double _y) {
        int width = WindowUtil.normalWidth();
        int height = WindowUtil.normalHeight();

        var mx = -2 * (_x / width - .5) / get(P, 0);
        var my = 2 * (_y / height - .5) / get(P, 5);

        var camX = - (get(V, 12) * get(V, 0) + get(V, 13) * get(V, 1) + get(V, 14) * get(V, 2));
        var camY = - (get(V, 12) * get(V, 4) + get(V, 13) * get(V, 5) + get(V, 14) * get(V, 6));
        var camZ = - (get(V, 12) * get(V, 8) + get(V, 13) * get(V, 9) + get(V, 14) * get(V, 10));

        if (get(P, 15) == 0) {    //This is a perspective projection
            return new double[]{get(V, 2)  + mx * get(V, 0) + my * get(V, 1),
                get(V, 6)  + mx * get(V, 4) + my * get(V, 5),
                get(V, 10) + mx * get(V, 8) + my * get(V, 9),
                camX,
                camY,
                camZ};
        } else {   //This is an ortho projection
            return new double[]{get(V, 2),
                get(V, 6),
                get(V, 10),
                camX + mx * get(V, 0) + my * get(V, 1),
                camY + mx * get(V, 4) + my * get(V, 5),
                camZ + mx * get(V, 8) + my * get(V, 9)};
        }
    }

    private static double get(Matrix4f m, int pos){
        int row = pos % 4;
        int column = pos / 4;
        return m.get(column, row);
    }
}
