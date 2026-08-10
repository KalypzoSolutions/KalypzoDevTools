package fish.crafting.fimfabric.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class KeyUtil {
    public static boolean isAltPressed(){
        return isPressed(InputUtil.GLFW_KEY_LEFT_ALT);
    }
    public static boolean isShiftPressed(){
        return isPressed(InputUtil.GLFW_KEY_LEFT_SHIFT);
    }
    public static boolean isControlPressed(){
        return isPressed(InputUtil.GLFW_KEY_LEFT_CONTROL);
    }


    public static boolean isPressed(int key){
        return McCompat.isKeyPressed(key);
    }

}
