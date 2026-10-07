package fish.crafting.fimfabric.listener;

import fish.crafting.fimfabric.keybind.CustomKeybind;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class KeybindTickListener implements ClientTickEvents.EndTick {

    private final CustomKeybind[] keybinds;
    private int totalTicks = 0;

    public KeybindTickListener(CustomKeybind... keybinds){
        this.keybinds = keybinds;
    }

    @Override
    public void onEndTick(Minecraft client) {
        for (CustomKeybind keybind : keybinds) {
            if(keybind.keyBinding.consumeClick()) {
                keybind.onPressed();
            }
        }

        totalTicks++;

    }
}
