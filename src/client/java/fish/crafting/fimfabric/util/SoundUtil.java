package fish.crafting.fimfabric.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class SoundUtil {

    public static void play(SoundEvent event){
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0f));
    }

    public static void play(SoundEvent event, float pitch){
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
    }

    public static void clickSound() {
        play(SoundEvents.UI_BUTTON_CLICK.value());
    }
}
