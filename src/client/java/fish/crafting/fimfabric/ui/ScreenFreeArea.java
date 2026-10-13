package fish.crafting.fimfabric.ui;

import fish.crafting.fimfabric.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Optional;

/**
 * Tells the parts of a screen that react to a click from the ones a right click can be taken from
 * without the screen, or the server behind it, missing it.
 */
public class ScreenFreeArea {

    public static boolean isFree(Screen screen, double x, double y){
        if(screen instanceof AbstractContainerScreen<?> container){
            if(((AbstractContainerScreenAccessor) container).getFimHoveredSlot() != null) return false;
            //With an item on the cursor every click places or drops it
            if(!container.getMenu().getCarried().isEmpty()) return false;
        }

        return !hitsControl(screen, x, y);
    }

    private static boolean hitsControl(ContainerEventHandler parent, double x, double y){
        Optional<GuiEventListener> child = parent.getChildAt(x, y);
        if(child.isEmpty()) return false;

        GuiEventListener hit = child.get();
        if(hit instanceof ContainerEventHandler nested) return hitsControl(nested, x, y);
        //Plain text does nothing on a right click
        return !(hit instanceof AbstractStringWidget);
    }

}
