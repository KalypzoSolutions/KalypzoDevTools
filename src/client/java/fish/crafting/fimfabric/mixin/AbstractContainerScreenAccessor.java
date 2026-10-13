package fish.crafting.fimfabric.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("hoveredSlot")
    @Nullable Slot getFimHoveredSlot();

    @Accessor("leftPos")
    int getFimLeftPos();

    @Accessor("topPos")
    int getFimTopPos();

    @Accessor("imageWidth")
    int getFimImageWidth();

    @Accessor("imageHeight")
    int getFimImageHeight();

    @Accessor("titleLabelX")
    int getFimTitleLabelX();

    @Accessor("titleLabelY")
    int getFimTitleLabelY();

}
