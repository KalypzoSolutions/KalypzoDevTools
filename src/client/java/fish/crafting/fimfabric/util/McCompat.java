package fish.crafting.fimfabric.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Style;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The handful of Minecraft calls that moved rather than just being renamed, so the rest of the
 * mod can stay version-agnostic. Everything else is handled by the preprocessor's mappings.
 */
public class McCompat {

    /**
     * Screen and overlay management moved from MinecraftClient onto its Gui in 26.2.
     */
    public static @Nullable Screen currentScreen(){
        //#if MC>=260200
        //$$ return Minecraft.getInstance().gui.screen();
        //#else
        return MinecraftClient.getInstance().currentScreen;
        //#endif
    }

    public static void setScreen(@Nullable Screen screen){
        //#if MC>=260200
        //$$ Minecraft.getInstance().gui.setScreen(screen);
        //#else
        MinecraftClient.getInstance().setScreen(screen);
        //#endif
    }

    public static boolean hasOverlay(){
        //#if MC>=260200
        //$$ return Minecraft.getInstance().gui.overlay() != null;
        //#else
        return MinecraftClient.getInstance().getOverlay() != null;
        //#endif
    }

    /**
     * F3+B. In 26.2 the debug toggles live in the debug screen entry list.
     */
    public static boolean isRenderingHitboxes(){
        //#if MC>=260200
        //$$ return Minecraft.getInstance().debugEntries.isCurrentlyEnabled(net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES);
        //#else
        return MinecraftClient.getInstance().getEntityRenderDispatcher().shouldRenderHitboxes();
        //#endif
    }

    /**
     * 26.2 takes the Window itself instead of its GLFW handle.
     */
    public static boolean isKeyPressed(int key){
        //#if MC>=260200
        //$$ return com.mojang.blaze3d.platform.InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
        //#else
        return net.minecraft.client.util.InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), key);
        //#endif
    }

    /**
     * Opens an empty chat screen. 26.2 added an "is draft" flag to the constructor.
     */
    public static void openEmptyChatScreen(){
        //#if MC>=260200
        //$$ setScreen(new net.minecraft.client.gui.screens.ChatScreen("", false));
        //#else
        setScreen(new net.minecraft.client.gui.screen.ChatScreen(""));
        //#endif
    }

    /**
     * Fonts are a FontDescription in 26.2, of which a font file is only one kind.
     */
    public static boolean isDefaultFont(Style style){
        //#if MC>=260200
        //$$ return net.minecraft.network.chat.FontDescription.DEFAULT.equals(style.getFont());
        //#else
        return Style.DEFAULT_FONT_ID.equals(style.getFont());
        //#endif
    }

    /**
     * The widgets of a screen, which mods may add to. Fabric renamed the method in 26.2.
     */
    public static List<ClickableWidget> screenWidgets(Screen screen){
        //#if MC>=260200
        //$$ return net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen);
        //#else
        return net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen);
        //#endif
    }

    /**
     * BlockPos#toCenterPos was dropped in 26.2.
     */
    public static Vec3d centerOf(BlockPos pos){
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /**
     * The camera entity is only reachable through a getter in 26.2.
     */
    public static Vec3d cameraRotation(){
        //#if MC>=260200
        //$$ return Minecraft.getInstance().getCameraEntity().getForward();
        //#else
        return MinecraftClient.getInstance().cameraEntity.getRotationVecClient();
        //#endif
    }

}
