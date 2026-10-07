package fish.crafting.fimfabric.connection.packets;

import fish.crafting.fimfabric.connection.packetsystem.InPacket;
import fish.crafting.fimfabric.rendering.RenderingManager;
import io.netty.buffer.ByteBufInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;

public class I2FFocusPacket implements InPacket {
    @Override
    public void readAndExecute(ByteBufInputStream stream) throws IOException {
        Gui gui = Minecraft.getInstance().gui;
        Screen screen = gui.screen();

        if(screen == null) return; //Already focused in-game

        if(screen instanceof ChatScreen || screen instanceof PauseScreen) {
            RenderingManager.get().addTask(() -> {
                gui.setScreen(null);
            });
        }
    }
}
