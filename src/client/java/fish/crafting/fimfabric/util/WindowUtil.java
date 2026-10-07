package fish.crafting.fimfabric.util;

import net.minecraft.client.Minecraft;

public class WindowUtil {

    public static int scaledWidth(){
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static int scaledHeight(){
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public static int normalWidth(){
        return Minecraft.getInstance().getWindow().getScreenWidth();
    }

    public static int normalHeight(){
        return Minecraft.getInstance().getWindow().getScreenHeight();
    }

    public static int guiScale(){
        Integer value = Minecraft.getInstance().options.guiScale().get();
        return Minecraft.getInstance().getWindow().calculateScale(value, false);
    }

}
