package fish.crafting.fimfabric.mixin;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The selection of a multi line text field has no public getter for its start.
 */
@Mixin(MultilineTextField.class)
public interface MultilineTextFieldAccessor {

    @Accessor("selectCursor")
    int getFimSelectCursor();

}
