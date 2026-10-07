package fish.crafting.fimfabric.util;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;

public class KeyUtil {
    public static boolean isAltPressed(){
        return isPressed(InputConstants.KEY_LALT);
    }
    public static boolean isShiftPressed(){
        return isPressed(InputConstants.KEY_LSHIFT);
    }
    public static boolean isControlPressed(){
        return isPressed(InputConstants.KEY_LCONTROL);
    }


    public static boolean isPressed(int key){
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }

}
