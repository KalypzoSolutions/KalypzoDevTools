package fish.crafting.fimfabric.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import fish.crafting.fimfabric.util.CursorPicking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//26.2
//
// The camera is passed as a CameraRenderState now and there is only one matrix parameter left; the
// projection matrix lives on the camera state. RenderSystem#getModelViewMatrix is gone, but the
// model view matrix is exactly what gets handed in here, so CursorPicking can take it directly.
@Mixin(LevelRenderer.class)
public class WorldRendererMixin {

    private static final Matrix4f IDENTITY = new Matrix4f().identity();

    @Inject(method = "render", at = @At("HEAD"))
    public void render(GraphicsResourceAllocator allocator,
                       DeltaTracker deltaTracker,
                       boolean renderOutline,
                       CameraRenderState cameraState,
                       Matrix4fc modelViewMatrix,
                       GpuBufferSlice terrainFog,
                       Vector4f fogColor,
                       boolean shouldRenderSky,
                       CallbackInfo ci){
        Matrix4f vm = new Matrix4f(modelViewMatrix);
        if(!vm.equals(IDENTITY, 0.00001f)){
            CursorPicking.viewMatrix.set(vm);
        }

        CursorPicking.positionMatrix.set(vm);
        CursorPicking.projectionMatrix.set(cameraState.projectionMatrix);
        CursorPicking.update();
    }

}
