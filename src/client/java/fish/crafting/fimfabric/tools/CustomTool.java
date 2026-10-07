package fish.crafting.fimfabric.tools;

import fish.crafting.fimfabric.client.FIMModClient;
import fish.crafting.fimfabric.rendering.custom.RenderContext3D;
import fish.crafting.fimfabric.rendering.custom.ScreenRenderContext;
import fish.crafting.fimfabric.tools.selector.WorldSelector;
import fish.crafting.fimfabric.util.Constants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public abstract class CustomTool<T extends Positioned> {

    public void onEnable() {

    }

    public void render2D(ScreenRenderContext context, DeltaTracker counter) {

    }

    public final void handleRender(RenderContext3D context, Positioned obj){
        render(context, (T) obj); //Oh well;
    }

    protected abstract void render(RenderContext3D context, T obj);

    public abstract boolean isAccessibleFor(Object object);
    public abstract Identifier getTexture();

    protected final double calculateToolZoom(Vec3 camera, Positioned positioned) {
        Vec3 pos = positioned.getPos();
        double d = camera.distanceToSqr(pos) / Constants.TOOL_OPTIMAL_RENDER_DISTANCE;

        return d <= 1.0 ? 1.0 : Math.sqrt(d);
    }

    public List<WorldSelector> getSelectors() {
        return null;
    }

    public boolean onScroll(double scroll) {
        return false;
    }

    public final void handleConfirmEdit(Positioned positioned){
        try{
            T cast = (T) positioned;
            confirmEdit(cast);
        }catch (Exception e) {
            FIMModClient.LOGGER.error("Unable to confirm edit for object '" + positioned + "', for tool " + this + "!", e);
        }
    }

    protected abstract void confirmEdit(T positioned);
}
