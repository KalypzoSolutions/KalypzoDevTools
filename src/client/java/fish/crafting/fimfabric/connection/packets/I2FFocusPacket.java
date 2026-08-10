package fish.crafting.fimfabric.connection.packets;

import fish.crafting.fimfabric.util.McCompat;
import fish.crafting.fimfabric.connection.packetsystem.InPacket;
import fish.crafting.fimfabric.rendering.RenderingManager;
import io.netty.buffer.ByteBufInputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;

import java.io.IOException;

public class I2FFocusPacket implements InPacket {
    @Override
    public void readAndExecute(ByteBufInputStream stream) throws IOException {
        MinecraftClient instance = MinecraftClient.getInstance();

        if(McCompat.currentScreen() == null) return; //Already focused in-game

        if(McCompat.currentScreen() instanceof ChatScreen || McCompat.currentScreen() instanceof GameMenuScreen) {
            RenderingManager.get().addTask(() -> {
                McCompat.setScreen(null);
            });
        }
    }
}
