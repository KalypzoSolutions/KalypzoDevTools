package fish.crafting.fimfabric.mixin;

//#if MC>=12106
import com.mojang.blaze3d.buffers.GpuBufferSlice;
//#endif
import com.mojang.blaze3d.systems.RenderSystem;
import fish.crafting.fimfabric.util.CursorPicking;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    private static final Matrix4f IDENTITY = new Matrix4f().identity();

    //#if MC<12106
    //$$
    //$$
    //$$
    //$$    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;mul(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;", shift = At.Shift.AFTER))
    //$$    public void render(ObjectAllocator allocator,
    //$$                       RenderTickCounter tickCounter,
    //$$                       boolean renderBlockOutline,
    //$$                       Camera camera,
    //$$                       GameRenderer gameRenderer,
    //$$                       Matrix4f positionMatrix,
    //$$                       Matrix4f projectionMatrix,
    //$$                       CallbackInfo ci){
    //$$        Matrix4f vm = new Matrix4f(RenderSystem.getModelViewMatrix());
    //$$        if(!vm.equals(IDENTITY, 0.00001f)){
    //$$            CursorPicking.viewMatrix.set(vm);
    //$$        }
    //$$
    //$$        MatrixStack matrix = new MatrixStack();
    //$$        matrix.multiplyPositionMatrix(positionMatrix);
    //$$        CursorPicking.positionMatrix.set(matrix.peek().getPositionMatrix());
    //$$
    //$$        CursorPicking.projectionMatrix.set(RenderSystem.getProjectionMatrix());
    //$$        CursorPicking.update();
    //$$ }
    //#else
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;mul(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;", shift = At.Shift.AFTER))
    public void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci){
        Matrix4f vm = new Matrix4f(RenderSystem.getModelViewMatrix());
        if(!vm.equals(IDENTITY, 0.00001f)){
            CursorPicking.viewMatrix.set(vm);
        }

        MatrixStack matrix = new MatrixStack();
        matrix.multiplyPositionMatrix(positionMatrix);
        CursorPicking.positionMatrix.set(matrix.peek().getPositionMatrix());

        CursorPicking.projectionMatrix.set(projectionMatrix);
        CursorPicking.update();
    }
    //#endif

}
