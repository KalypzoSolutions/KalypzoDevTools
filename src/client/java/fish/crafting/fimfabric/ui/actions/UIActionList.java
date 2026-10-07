package fish.crafting.fimfabric.ui.actions;

import fish.crafting.fimfabric.rendering.custom.ScreenRenderContext;
import fish.crafting.fimfabric.ui.FancyText;
import fish.crafting.fimfabric.ui.UIComponent;
import fish.crafting.fimfabric.util.ClickContext;
import fish.crafting.fimfabric.util.MouseUtil;
import fish.crafting.fimfabric.util.WindowUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static fish.crafting.fimfabric.ui.actions.ActionElement.X_PADDING;

public class UIActionList extends UIComponent {

    protected static final int WIDTH_LARGE = 280, WIDTH_MEDIUM = 200, WIDTH_SMALL = 120;

    private boolean separateNextAddedElement = false;

    protected int enabledElementCount = 0;
    protected final List<ActionElement> elements = new ArrayList<>();
    private UIActionList expandedChild = null;
    @Getter
    private ActionElement expandedWhoCalled = null;
    private @Nullable FancyText title;
    private boolean returnToGameScreenAfter = false;

    public UIActionList(int width) {
        super(0, 0, width, 0);
    }

    @Override
    protected void render(ScreenRenderContext context) {
        int bg = 0x99000000;
        fill(context, bg);

        context.nextLayer();

        int height = heightPerElement();
        context.drawBorder(renderX - 1, renderY, renderWidth + 1, height * enabledElementCount, 0xFFFFFFFF);

        if(title != null){
            context.fill(
                    renderX,
                    renderY,
                    renderX + renderWidth,
                    renderY - height,
                    bg
            );

            context.drawBorder(
                    renderX - 1,
                    renderY - height,
                    renderWidth + 1,
                    height + 1,
                    0xFFFFFFFF);

            Font textRenderer = Minecraft.getInstance().font;
            int centerY = renderY - height / 2 - textRenderer.lineHeight / 2;

            title.render(context,
                    renderX + X_PADDING,
                    centerY,
                    0xFFFFFFFF,
                    false);
        }
    }

    protected final void addSeparator(){
        separateNextAddedElement = true;
    }

    protected final <T extends ActionElement> T addElement(@NotNull T element){
        elements.add(element);
        addChildren(element);

        if(separateNextAddedElement){
            separateNextAddedElement = false;
            element.separator(true);
        }

        return element;
    }

    protected final ActionElement addRunElement(@NotNull FancyText text, @NotNull ContextRunnable runnable){
        return addElement(new ActionElement(text) {
            @Override
            protected void activate(ClickContext context) {
                runnable.activate(context);
            }
        });
    }

    public UIActionList title(FancyText text){
        this.title = text;
        return this;
    }

    protected final ExpandActionElement addExpandElement(@NotNull FancyText text, @NotNull Supplier<UIActionList> expandedListGetter){
        return addElement(new ExpandActionElement(text) {
            @Override
            protected UIActionList getExpandedList() {
                return expandedListGetter.get();
            }
        });
    }

    private int heightPerElement(){
        return (int) (Minecraft.getInstance().font.lineHeight * 1.5 + 1);
    }

    public void showListAtMouse(){
        showListAtMouse(false);
    }

    public void showListAtMouse(boolean returnToGameScreenAfter){
        showList(
                MouseUtil.scaledXInt(),
                MouseUtil.scaledYInt(),
                returnToGameScreenAfter
        );
    }

    public void showList(int x, int y){
        showList(x, y, false);
    }

    public void showList(int x, int y, boolean returnToGameScreenAfter){
        this.returnToGameScreenAfter = returnToGameScreenAfter;
        move(x, y, width, height);
        enable();
    }

    /**
     *
     * @deprecated Use {@code showListAtMouse()} or {@code showList(Int, Int)} instead.
     */
    @Override @Deprecated
    public UIComponent enable() {
        return super.enable();
    }

    @Override
    protected void onEnable(boolean wasDisabled) {
        expand(null, null);
        update(x, y);
        getChildren().forEach(UIComponent::enable);
        enabledElementCount = enabledChildren.size();
    }

    @Override
    protected void onDisable(boolean wasEnabled) {
        expand(null, null);
        if(returnToGameScreenAfter && wasEnabled){
            returnToGameScreenAfter = false;
            Minecraft.getInstance().gui.setScreen(null);
        }
    }

    public void update(int x, int y){
        int heightPerElement = heightPerElement();

        int _y = 0;
        for (ActionElement element : elements) {
            element.update();

            if(element.visible) {
                element.enable();
                element.move(x, y + _y, width, heightPerElement);
                _y += heightPerElement;
            }else{
                element.disable();
            }
        }
    }

    public void expand(UIActionList expandedList, ActionElement whoCalled) {
        if(this.expandedChild == expandedList) return;
        expandedWhoCalled = whoCalled;

        if(this.expandedChild != null){
            removeChild(this.expandedChild);
            this.expandedChild.disableAll();
        }

        this.expandedChild = expandedList;

        if(this.expandedChild != null){
            addChildren(this.expandedChild);
            this.expandedChild.enable();
        }
    }

    public boolean hasExpanded() {
        return this.expandedChild != null && this.expandedChild.isEnabled();
    }

    protected interface ContextRunnable{
        void activate(ClickContext context);
    }
}
