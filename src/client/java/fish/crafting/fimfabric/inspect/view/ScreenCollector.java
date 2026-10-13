package fish.crafting.fimfabric.inspect.view;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import fish.crafting.fimfabric.mixin.AbstractContainerScreenAccessor;
import fish.crafting.fimfabric.mixin.DialogScreenAccessor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.Nullable;

public class ScreenCollector {

    public static JsonElement collect(TextEncoder encoder, @Nullable Screen screen){
        if(screen == null) return JsonNull.INSTANCE;

        JsonObject json = new JsonObject();
        json.addProperty("class", screen.getClass().getName());
        json.add("title", encoder.text(screen.getTitle(), "screen.title"));

        if(screen instanceof DialogScreen<?> dialogScreen){
            json.addProperty("type", "dialog");
            Dialog dialog = ((DialogScreenAccessor) dialogScreen).getFimDialog();
            json.add("dialog", encoder.encode(Dialog.DIRECT_CODEC, dialog));
        }else if(screen instanceof AbstractContainerScreen<?> containerScreen){
            json.addProperty("type", "container");
            json.add("container", container(encoder, containerScreen));
        }else{
            json.addProperty("type", "other");
        }

        JsonArray widgets = new JsonArray();
        widgets(encoder, screen, widgets);
        json.add("widgets", widgets);

        return json;
    }

    private static JsonObject container(TextEncoder encoder, AbstractContainerScreen<?> screen){
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        AbstractContainerMenu menu = screen.getMenu();

        JsonObject json = new JsonObject();
        json.addProperty("menuType", menuType(menu));
        json.addProperty("left", accessor.getFimLeftPos());
        json.addProperty("top", accessor.getFimTopPos());
        json.addProperty("width", accessor.getFimImageWidth());
        json.addProperty("height", accessor.getFimImageHeight());
        json.addProperty("titleX", accessor.getFimTitleLabelX());
        json.addProperty("titleY", accessor.getFimTitleLabelY());

        if(!menu.getCarried().isEmpty()){
            json.add("carried", item(encoder, menu.getCarried(), "screen.container.carried"));
        }

        JsonArray slots = new JsonArray();
        for (Slot slot : menu.slots) {
            JsonObject slotJson = new JsonObject();
            slotJson.addProperty("index", slot.index);
            slotJson.addProperty("inventory", slot.container instanceof Inventory ? "player" : "container");
            slotJson.addProperty("inventorySlot", slot.getContainerSlot());
            //Relative to left/top
            slotJson.addProperty("x", slot.x);
            slotJson.addProperty("y", slot.y);

            ItemStack stack = slot.getItem();
            if(!stack.isEmpty()){
                slotJson.add("item", item(encoder, stack, "screen.container.slots[" + slot.index + "].item"));
            }

            slots.add(slotJson);
        }
        json.add("slots", slots);

        return json;
    }

    private static @Nullable String menuType(AbstractContainerMenu menu){
        try{
            Identifier key = BuiltInRegistries.MENU.getKey(menu.getType());
            return key == null ? null : key.toString();
        }catch (UnsupportedOperationException e){
            //The player's own inventory has no type
            return null;
        }
    }

    private static JsonObject item(TextEncoder encoder, ItemStack stack, String where){
        JsonObject json = new JsonObject();
        json.add("stack", encoder.encode(ItemStack.CODEC, stack));
        json.add("name", encoder.text(stack.getHoverName(), where + ".name"));

        ItemLore lore = stack.get(DataComponents.LORE);
        if(lore != null && !lore.lines().isEmpty()){
            JsonArray lines = new JsonArray();
            for (Component line : lore.lines()) {
                lines.add(encoder.text(line, where + ".lore[" + lines.size() + "]"));
            }
            json.add("lore", lines);
        }

        return json;
    }

    private static void widgets(TextEncoder encoder, ContainerEventHandler parent, JsonArray out){
        for (GuiEventListener child : parent.children()) {
            if(child instanceof AbstractWidget widget){
                JsonObject json = new JsonObject();
                json.addProperty("class", widget.getClass().getName());
                json.addProperty("x", widget.getX());
                json.addProperty("y", widget.getY());
                json.addProperty("width", widget.getWidth());
                json.addProperty("height", widget.getHeight());
                if(!widget.visible) json.addProperty("visible", false);
                if(!widget.active) json.addProperty("active", false);

                Component message = widget.getMessage();
                if(message != null && !message.getString().isEmpty()){
                    json.add("message", encoder.text(message, "screen.widgets[" + out.size() + "].message"));
                }

                out.add(json);
            }

            if(child instanceof ContainerEventHandler nested) widgets(encoder, nested, out);
        }
    }

}
