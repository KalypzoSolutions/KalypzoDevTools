package fish.crafting.fimfabric.inspect.item;

import fish.crafting.fimfabric.mixin.AbstractContainerScreenAccessor;
import fish.crafting.fimfabric.rendering.InformationFeedManager;
import fish.crafting.fimfabric.util.SoundUtil;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds out what the player means by "inspect this" and opens the matching screen: the item under
 * the cursor in a container, otherwise the block in the crosshair, otherwise the held item.
 */
public class ItemInspectManager {

    private static ItemInspectManager instance;

    //Set by the keybinds, they only exist once the client initialised
    private @Nullable KeyMapping inspectKey;
    private @Nullable KeyMapping editKey;

    private ItemInspectManager(){
        instance = this;

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if(!(screen instanceof AbstractContainerScreen<?> container)) return;

            //Keybinds do not fire while a screen is open, so the screen has to be asked
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
                //Typing a name into the anvil or a search must not open anything
                if(s.getFocused() instanceof EditBox || s.getFocused() instanceof MultiLineEditBox) return true;

                boolean inspect = inspectKey != null && inspectKey.matches(event);
                boolean edit = editKey != null && editKey.matches(event);
                if(!inspect && !edit) return true;

                Slot slot = ((AbstractContainerScreenAccessor) container).getFimHoveredSlot();
                if(slot == null || !slot.hasItem()) return true;

                if(edit) edit(slot.getItem());
                else inspect(slot.getItem());
                return false;
            });
        });
    }

    public static ItemInspectManager get(){
        return instance == null ? new ItemInspectManager() : instance;
    }

    public void bind(@NotNull KeyMapping inspectKey, @NotNull KeyMapping editKey){
        this.inspectKey = inspectKey;
        this.editKey = editKey;
    }

    /**
     * For the keybind: no screen is open, so the world is the target.
     */
    public void inspectInWorld(){
        Minecraft mc = Minecraft.getInstance();
        if(mc.level == null || mc.player == null) return;
        if(!requireOwo()) return;

        if(mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK){
            BlockState state = mc.level.getBlockState(hit.getBlockPos());
            InspectorScreen.open(Inspection.ofBlock(mc.level, hit.getBlockPos(), state));
            SoundUtil.clickSound();
            return;
        }

        ItemStack held = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if(held.isEmpty()){
            InformationFeedManager.warn("Nothing to inspect: look at a block or hold an item", false);
            return;
        }
        inspect(held);
    }

    public void editInWorld(){
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) return;

        ItemStack held = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if(held.isEmpty()){
            InformationFeedManager.warn("Hold the item you want to edit", false);
            return;
        }
        edit(held);
    }

    private void inspect(ItemStack stack){
        if(!requireOwo()) return;
        InspectorScreen.open(Inspection.ofItem(stack));
        SoundUtil.clickSound();
    }

    private void edit(ItemStack stack){
        if(!requireOwo()) return;
        ItemEditorScreen.open(stack);
        SoundUtil.clickSound();
    }

    private boolean requireOwo(){
        //owo is optional, without it the screens have nothing to build on
        if(FabricLoader.getInstance().isModLoaded("owo")) return true;

        InformationFeedManager.error("The inspector needs owo-lib", true);
        return false;
    }

}
