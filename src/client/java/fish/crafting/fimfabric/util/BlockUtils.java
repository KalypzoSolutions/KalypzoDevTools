package fish.crafting.fimfabric.util;

import fish.crafting.fimfabric.util.cache.RenderFrameCache;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

public class BlockUtils {

    private static final int MAX_RAY_DISTANCE = 64;

    private static final RenderFrameCache<BlockPos> PLAYER_TARGET_CACHE = new RenderFrameCache<>(null);

    public static BlockPos getTargetBlockPos() {
        return PLAYER_TARGET_CACHE.computeIfAbsent(BlockUtils::getPlayerTargetPos);
    }

    private static BlockPos getPlayerTargetPos(){
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if(world == null) return null;

        CollisionContext context = (client.player == null ? CollisionContext.empty() : CollisionContext.of(client.player));

        Camera camera = client.gameRenderer.mainCamera();
        Vec3 pos = camera.position();
        Vec3 rot = client.getCameraEntity().getForward();
        Vec3 pos2 = pos.add(rot.scale(MAX_RAY_DISTANCE));

        var fluidHandling = ClipContext.Fluid.NONE;
        var shapeType = ClipContext.Block.COLLIDER;
        if(KeyUtil.isShiftPressed()) {
            fluidHandling = ClipContext.Fluid.ANY;
            shapeType = ClipContext.Block.OUTLINE;
        }

        BlockHitResult raycast = world.clip(new ClipContext(pos, pos2, shapeType, fluidHandling, context));
        return raycast.getBlockPos();
    }

}
