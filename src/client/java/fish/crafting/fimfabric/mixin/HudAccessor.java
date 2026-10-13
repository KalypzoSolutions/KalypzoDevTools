package fish.crafting.fimfabric.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface HudAccessor {

    @Accessor("title")
    @Nullable Component getFimTitle();

    @Accessor("subtitle")
    @Nullable Component getFimSubtitle();

    @Accessor("titleTime")
    int getFimTitleTime();

    @Accessor("titleFadeInTime")
    int getFimTitleFadeInTime();

    @Accessor("titleStayTime")
    int getFimTitleStayTime();

    @Accessor("titleFadeOutTime")
    int getFimTitleFadeOutTime();

    @Accessor("overlayMessageString")
    @Nullable Component getFimOverlayMessage();

    @Accessor("overlayMessageTime")
    int getFimOverlayMessageTime();

    @Accessor("animateOverlayMessageColor")
    boolean getFimAnimateOverlayMessageColor();

}
