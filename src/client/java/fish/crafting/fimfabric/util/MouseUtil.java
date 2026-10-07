package fish.crafting.fimfabric.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import com.mojang.blaze3d.platform.Window;

public class MouseUtil {

    public static double x(){
        return Minecraft.getInstance().mouseHandler.xpos();
    }

    public static double y(){
        return Minecraft.getInstance().mouseHandler.ypos();
    }

    public static int xInt(){
        return (int) x();
    }

    public static int yInt(){
        return (int) y();
    }

    public static int scaledXInt(){
        Window window = Minecraft.getInstance().getWindow();
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
        return (int) scaleX(window, mouse.xpos());
    }

    public static int scaledYInt(){
        Window window = Minecraft.getInstance().getWindow();
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
        return (int) scaleY(window, mouse.ypos());
    }

    private static double scaleX(Window window, double x) {
        return x * (double)window.getGuiScaledWidth() / (double)window.getScreenWidth();
    }

    private static double scaleY(Window window, double y) {
        return y * (double)window.getGuiScaledHeight() / (double)window.getScreenHeight();
    }

}
