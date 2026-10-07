package fish.crafting.fimfabric.util;

import fish.crafting.fimfabric.rendering.InformationFeedManager;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

public class ActionUtils {

    public static void copyIfClick(ClickContext context, Supplier<String> copy){
        if(!context.isLeftClickPress()) return;

        Minecraft.getInstance().keyboardHandler.setClipboard(copy.get());
        InformationFeedManager.success("Copied!", false);
        SoundUtil.clickSound();
    }

}
