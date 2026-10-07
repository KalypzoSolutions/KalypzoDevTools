package fish.crafting.fimfabric.ui.actions;

import fish.crafting.fimfabric.rendering.custom.RenderContext3D;
import fish.crafting.fimfabric.rendering.custom.ScreenRenderContext;
import fish.crafting.fimfabric.ui.FancyText;
import fish.crafting.fimfabric.util.BlockUtils;
import fish.crafting.fimfabric.util.ClickContext;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * Lets the user select a block to use this action element on.
 * Normally, this will select the block the player is looking at.
 */
public abstract class BlockPosActionElement extends ActionElement {
    public BlockPosActionElement(FancyText text) {
        super(text);
    }

    @Override
    protected final void activate(ClickContext context) {
        activate(context, getPos());
    }

    public static BlockPos getPos(){
        return BlockUtils.getTargetBlockPos();
    }

    @Override
    protected void render(ScreenRenderContext context) {
        super.render(context);
    }

    protected abstract void activate(ClickContext context, BlockPos pos);

    /**
     * This gets called when this action is hovered.
     */
    public void renderWorldSpace(@NotNull RenderContext3D context) {
        BlockPos pos = getPos();

        context.push();
        context.translateCamera();

        context.renderBoxOutline(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                1f, 1f, 1f, 1f);

        context.pop();
    }
}
