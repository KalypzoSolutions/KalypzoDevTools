package fish.crafting.fimfabric.client;

import fish.crafting.fimfabric.listener.ConnectionShutdownListener;
import fish.crafting.fimfabric.listener.GenericTickListener;
import fish.crafting.fimfabric.listener.KeybindTickListener;
import fish.crafting.fimfabric.connection.packetsystem.PacketManager;
import fish.crafting.fimfabric.keybind.*;
import fish.crafting.fimfabric.rendering.world.WorldRenderingManager;
import fish.crafting.fimfabric.titleoffset.TitleOffsetEditor;
import fish.crafting.fimfabric.tools.render.ToolRenderingManager;
import fish.crafting.fimfabric.util.SoundManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.sound.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FIMModClient implements ClientModInitializer {

    public static final String NAMESPACE = "fim";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);


    @Override
    public void onInitializeClient() {
        PacketManager.get();

        ClientLifecycleEvents.CLIENT_STOPPING.register(new ConnectionShutdownListener());
        ClientTickEvents.END_CLIENT_TICK.register(new GenericTickListener());
        ClientTickEvents.END_CLIENT_TICK.register(new KeybindTickListener(
                new SwitchToIntelliJKeybind(),
                new ConfirmEditKeybind(),
                new MoveToolKeybind(),
                new RotateToolKeybind(),
                new QuickActionsKeybind(),
                new PreviousSnappingKeybind(),
                new NextSnappingKeybind(),
                new ScaleToolKeybind()
        ));

        ToolRenderingManager.get();
        WorldRenderingManager.get();
        TitleOffsetEditor.get();
        SoundEvent aSwitch = SoundManager.SWITCH;
    }
}
