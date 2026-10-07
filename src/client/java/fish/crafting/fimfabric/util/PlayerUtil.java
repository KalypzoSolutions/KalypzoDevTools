package fish.crafting.fimfabric.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerUtil {

    public static void sendCommand(@NotNull String command){
        ClientPacketListener networkHandler = Minecraft.getInstance().getConnection();
        if(networkHandler != null) {
            networkHandler.sendCommand(command);
        }
    }

    public static void teleportTo(double x, double y, double z){
        sendCommand("minecraft:teleport @s " + x + " " + y + " " + z);
    }

}
