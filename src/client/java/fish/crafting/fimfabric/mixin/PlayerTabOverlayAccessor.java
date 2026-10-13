package fish.crafting.fimfabric.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {

    @Accessor("header")
    @Nullable Component getFimHeader();

    @Accessor("footer")
    @Nullable Component getFimFooter();

}
