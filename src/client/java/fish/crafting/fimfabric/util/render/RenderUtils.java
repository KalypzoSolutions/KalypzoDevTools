package fish.crafting.fimfabric.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;

public class RenderUtils {

    public static float tickDelta(){
        return tickDelta(false);
    }

    public static float tickDelta(boolean ignoreFreeze){
        DeltaTracker counter = Minecraft.getInstance().getDeltaTracker();


        return counter.getGameTimeDeltaPartialTick(ignoreFreeze);
    }

}
