package fish.crafting.fimfabric.ui;

import fish.crafting.fimfabric.util.McCompat;
import fish.crafting.fimfabric.rendering.custom.ScreenRenderContext;
import fish.crafting.fimfabric.tools.CustomTool;
import fish.crafting.fimfabric.tools.EditorTools;
import fish.crafting.fimfabric.ui.actions.UIActionList;
import fish.crafting.fimfabric.ui.custom.UIClearFeedButton;
import fish.crafting.fimfabric.ui.custom.UILanguageSwitcher;
import fish.crafting.fimfabric.ui.custom.UIToolButton;
import fish.crafting.fimfabric.ui.custom.blockactions.UIBlockActions;
import fish.crafting.fimfabric.ui.custom.quickactions.UIQuickActions;
import fish.crafting.fimfabric.ui.scroll.Scroller;
import fish.crafting.fimfabric.ui.scroll.UIScrollable;
import fish.crafting.fimfabric.util.*;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class InterfaceManager {

    private static InterfaceManager instance = null;
    private static long cursor = -1L;
    private final UIBaseLayer BASE = new UIBaseLayer();
    private final UIBaseLayer MOUSE_DEPENDANT_LAYER = new UIBaseLayer();
    private final UIBaseLayer CHAT_DEPENDANT_LAYER = new UIBaseLayer();
    private final UIBaseLayer SCREENS = new UIBaseLayer();
    private final UIBaseLayer TOOLS = new UIBaseLayer();
    private final UILanguageSwitcher LANG_SWITCHER = new UILanguageSwitcher();

    //Only one *main* action list should be active at once.
    private UIActionList activeActionList = null;

    private final UIQuickActions QUICK_ACTIONS = new UIQuickActions();
    private final UIBlockActions BLOCK_ACTIONS = new UIBlockActions();

    @Getter
    private UIComponent currentlyHovering = null;
    private final Set<UIComponent> disabledButStillShouldRender = new HashSet<>();
    //Probably could be done better.
    private final Queue<Runnable> postRenderTaskQueue = new ArrayDeque<>();

    private boolean updateHover = false;

    private InterfaceManager(){
        instance = this;

        BASE.addChildren(SCREENS, QUICK_ACTIONS, BLOCK_ACTIONS);
        setupBasicInterface();
    }

    private void setupBasicInterface(){
        BASE.addChildren(CHAT_DEPENDANT_LAYER, MOUSE_DEPENDANT_LAYER);

        List<CustomTool<?>> allTools = EditorTools.getAll();
        int index = allTools.size() - 1;
        for (CustomTool<?> tool : allTools) {
            TOOLS.addChildren(new UIToolButton(index--, tool));
        }

        CHAT_DEPENDANT_LAYER.addChildren(
            TOOLS,
            LANG_SWITCHER
        );

        MOUSE_DEPENDANT_LAYER.addChildren(
                new UIClearFeedButton()
        );
    }

    public void addPostRenderTask(@NotNull Runnable runnable){
        postRenderTaskQueue.add(runnable);
    }

    public void scheduleHoverUpdate(){
        this.updateHover = true;
    }

    public void onChatOpenState(boolean open){
        if(open){
            CHAT_DEPENDANT_LAYER.enableAll();
        }else{
            CHAT_DEPENDANT_LAYER.disableAll();
        }
    }

    public void handleMouseLock(boolean locked){
        if(locked) {
            actionList(null);
        }

        if(locked){
            MOUSE_DEPENDANT_LAYER.disableAll();
        }else{
            MOUSE_DEPENDANT_LAYER.enableAll();
        }
    }

    public static InterfaceManager get(){
        return instance == null ? new InterfaceManager() : instance;
    }

    public void onResize(int width, int height){
        BASE.visitEnabled(c -> c.onWindowResized(width, height), true);
    }

    public void render(ScreenRenderContext context){
        if(this.updateHover){
            updateHover(MouseUtil.xInt(), MouseUtil.yInt());
            this.updateHover = false;
        }

        BASE.visitEnabled(c -> renderComponent(c, context), false);

        while(!postRenderTaskQueue.isEmpty()){
            postRenderTaskQueue.remove().run();
        }

        //funny but works
        if(!disabledButStillShouldRender.isEmpty()){
            disabledButStillShouldRender.removeIf(c -> c.renderDisabled(context));
        }
    }

    private void renderComponent(UIComponent component, ScreenRenderContext context){
        context.push();
        component.handleRender(context);

        Scroller scroller;
        if(component instanceof UIScrollable uiScrollable
                && (scroller = uiScrollable.scroller()) != null){

            int height = component.renderHeight;
            float scroll1 = scroller.getScroll();
            float scroll2 = scroller.getScroll() + height;

            float maxScroll = scroller.maxScroll() + height;

            double d1 = scroll1 / maxScroll;
            double d2 = scroll2 / maxScroll;

            int h1 = (int) (d1 * height);
            int h2 = (int) (d2 * height);

            context.drawVerticalLine(uiScrollable.scrollerPosition().getX(component),
                    component.renderY + h1,
                    component.renderY + h2,
                    0x88FFFFFF);
        }
        context.pop();
    }

    private void actionList(UIActionList list){
        actionList(list, false);
    }

    private void actionList(UIActionList list, boolean returnToGameScreenAfter){
        if(activeActionList == list) {
            if(list != null){
                list.showListAtMouse();
            }
            return;
        }

        if(activeActionList != null){
            activeActionList.disable();
        }

        activeActionList = list;

        if(list != null){
            list.showListAtMouse(returnToGameScreenAfter);
        }
    }

    public void handleScroll(double scroll) {
        if(!(currentlyHovering instanceof UIScrollable uiScrollable)) return;

        Scroller scroller = uiScrollable.scroller();
        if(scroller == null) return;

        scroller.addAnimatedScroll((float) scroll);
    }

    public void handleClick(int button, int action, int mods){
        boolean hoveringNothingOrBase = currentlyHovering == null || currentlyHovering instanceof UIBaseLayer;

        if(activeActionList != null && activeActionList.isEnabled() && action == 0) {
            if(hoveringNothingOrBase || currentlyHovering.topMostParent(true) != activeActionList) {
                actionList(null);
            }
        }

        if(hoveringNothingOrBase && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == 0){
            Screen currentScreen = McCompat.currentScreen();

            if(currentScreen instanceof ChatScreen) {
                if(CursorPicking.areBlockPickingPrerequisitesMet()){
                    actionList(BLOCK_ACTIONS);
                }else{
                    actionList(QUICK_ACTIONS);
                }
            }
        }

        if(currentlyHovering != null) {
            currentlyHovering.onClick(new ClickContext(button, action, mods));
        }
    }

    public boolean isBlockActionListActive(){
        return BLOCK_ACTIONS.isEnabled();
    }

    public UIComponent getTopmost(int x, int y, boolean filterHoverable){
        AtomicReference<UIComponent> topmost = new AtomicReference<>(null);
        AtomicInteger mostLayer = new AtomicInteger(-1);

        BASE.visitEnabledLayered((component, layer) -> {
            if(component.isWithin(x, y) && (component.isHoverable() || !filterHoverable) && layer > mostLayer.get()) {
                mostLayer.set(layer);
                topmost.set(component);
            }
        }, false, 0);

        return topmost.get();
    }

    public void openQuickActions(boolean returnToGameScreenAfter) {
        actionList(QUICK_ACTIONS, returnToGameScreenAfter);
    }

    private void updateHover(int x, int y){
        int gx = MouseUtil.scaledXInt();
        int gy = MouseUtil.scaledYInt();

        UIComponent hovered = getTopmost(gx, gy, true);

        if(hovered != currentlyHovering) {
            long now = System.nanoTime();

            if(currentlyHovering != null){
                currentlyHovering.endedHoveringAt = now;
            }

            currentlyHovering = hovered;

            if(hovered != null){
                hovered.startedHoveringAt = now;
            }

        }

        updateCursor();
    }

    public void updateCursor(){
        long newCursor = getCursor();
        if (newCursor != cursor) {
            cursor = newCursor;
            GLFW.glfwSetCursor(MinecraftClient.getInstance().getWindow().getHandle(), cursor);
        }
    }

    private long getCursor(){
        if(CursorPicking.areBlockPickingPrerequisitesMet()){
            return Cursors.POINTING;
        }

        if(currentlyHovering != null) {
            return currentlyHovering.getHoverCursor();
        }

        return Cursors.NORMAL;
    }

    public void handleCursorPos(int x, int y){
        updateHover(x, y);
    }

    public void handleEnabled(UIComponent uiComponent) {
        handleCursorPos(MouseUtil.xInt(), MouseUtil.yInt());
        disabledButStillShouldRender.remove(uiComponent);
        scheduleHoverUpdate();
    }

    public void handleDisabled(UIComponent uiComponent) {
        handleCursorPos(MouseUtil.xInt(), MouseUtil.yInt());
        if(isPartOfMainComponent(uiComponent) && uiComponent.shouldRenderWhenDisabled()) {
            disabledButStillShouldRender.add(uiComponent);
        }
        scheduleHoverUpdate();
    }

    /**
     * The interface renders through one common parent, the BASE component.
     * This method returns whether a certain UIComponent is some child of BASE.
     * Useful for checking if an element is still in the common parent's pool.
     */
    private boolean isPartOfMainComponent(UIComponent component){
        return component.topMostParent() == BASE;
    }

    public static class UIBaseLayer extends UIComponent {
        public UIBaseLayer() {
            super(0, 0, 0, 0);
        }

        @Override
        public void onWindowResized(int width, int height) {
            move(0, 0, width, height);
        }

        @Override
        public boolean isHoverable() {
            return false;
        }
    }

}
