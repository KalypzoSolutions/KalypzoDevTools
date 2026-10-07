package fish.crafting.fimfabric.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;

public class SoundManager {

    public static final SoundEvent SWITCH = register("switch");

    private static SoundEvent register(@NotNull String id){
        return Registry.register(BuiltInRegistries.SOUND_EVENT, ModUtil.identifier(id), SoundEvent.createVariableRangeEvent(ModUtil.identifier(id)));
    }

}
