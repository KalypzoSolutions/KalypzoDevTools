package fish.crafting.fimfabric.ui;

import fish.crafting.fimfabric.rendering.custom.ScreenRenderContext;
import fish.crafting.fimfabric.util.ClickContext;
import fish.crafting.fimfabric.util.Cursors;
import fish.crafting.fimfabric.util.NumUtil;
import fish.crafting.fimfabric.util.WindowUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class UIComponent {
    private static int ID = 0;
    protected final int internalID = ID++;
    @Getter
    private final List<UIComponent> children = new ArrayList<>();
    protected final List<UIComponent> enabledChildren = new ArrayList<>();

    protected @Getter int x = 0, y = 0;
    protected @Getter @Setter int width = 0, height = 0;
    //Utility fields for rendering methods
    public int renderX = 0, renderY = 0, renderHeight = 0, renderWidth = 0;
    @Getter
    private UIComponent parent = null;
    @Getter
    private boolean enabled = false;
    protected long startedHoveringAt = 0L;
    protected long endedHoveringAt = 0L;
    protected long lastEnableSwitchTime = 0L;
    @Getter
    private long hoverCursor = Cursors.NORMAL;

    private UIComponent(){
        onWindowResized(WindowUtil.scaledWidth(), WindowUtil.scaledHeight());
    }

    public UIComponent(int x, int y, int width, int height){
        this();
        move(x, y, width, height);
    }

    public void visitAll(Consumer<UIComponent> consumer, boolean visitSelf){
        if(visitSelf) consumer.accept(this);
        children.forEach(c -> c.visitAll(consumer, true));
    }

    public void visitEnabled(Consumer<UIComponent> consumer, boolean visitSelf){
        if(visitSelf) consumer.accept(this);
        enabledChildren.forEach(c -> {
            c.visitEnabled(consumer, true);
        });
    }

    public UIComponent hoverCursor(long cursor){
        this.hoverCursor = cursor;
        return this;
    }

    public UIComponent hoverCursorClick(){
        return hoverCursor(Cursors.POINTING);
    }

    public void visitEnabledLayered(BiConsumer<UIComponent, Integer> consumer, boolean visitSelf, int startLayer){
        if(visitSelf) consumer.accept(this, startLayer);
        children.forEach(c -> {
            if(c.isEnabled()){
                c.visitEnabledLayered(consumer, true, startLayer + 1);
            }
        });
    }

    public void visitParents(Consumer<UIComponent> consumer, boolean visitSelf) {
        if(visitSelf) consumer.accept(this);

        doWithParent(parent -> parent.visitParents(consumer, true));
    }

    public UIComponent enable(){
        if(!this.enabled){
            InterfaceManager.get().handleEnabled(this);

            if(parent != null){
                parent.enabledChildren.add(this);
            }

            lastEnableSwitchTime = System.nanoTime();

        }

        onWindowResized(WindowUtil.scaledWidth(), WindowUtil.scaledHeight());
        onEnable(!this.enabled);

        this.enabled = true;
        return this;
    }

    public UIComponent disable(){
        if(this.enabled){
            InterfaceManager.get().handleDisabled(this);
            lastEnableSwitchTime = System.nanoTime();

            if(parent != null){
                parent.enabledChildren.remove(this);
            }
        }

        onDisable(this.enabled);

        this.enabled = false;
        return this;
    }

    protected void onEnable(boolean wasDisabled){

    }

    protected void onDisable(boolean wasEnabled){

    }

    protected final int centerRenderedY(){
        return renderY + renderHeight / 2;
    }

    public UIComponent enableAll(){
        visitAll(UIComponent::enable, true);
        return this;
    }

    public UIComponent disableAll(){
        visitAll(UIComponent::disable, true);
        return this;
    }

    public UIComponent copyPositionSizeFromParent(){
        return doWithParent(parent -> {
            this.x = parent.x;
            this.y = parent.y;
            this.width = parent.width;
            this.height = parent.height;
        });
    }

    public boolean hasParent(){
        return parent != null;
    }

    public boolean isHovered(){
        return endedHoveringAt < startedHoveringAt;
    }

    public UIComponent firstParentThatMeets(@NotNull Predicate<UIComponent> predicate, boolean includeSelf){
        if(includeSelf && predicate.test(this)) return this;
        if(parent == null) return null;

        return parent.firstParentThatMeets(predicate, true);
    }

    public UIComponent doWithParent(@NotNull Consumer<UIComponent> parentConsumer){
        if(this.parent != null) parentConsumer.accept(this.parent);
        return this;
    }

    public boolean isWithin(int x, int y){
        return this.x <= x && x < (this.x + width) && this.y <= y && y < (this.y + height);
    }

    /**
     * Renders elements while they're disabled ONLY if renderWhenDisabled() is true
     *
     * @return true, if the component has finished rendering, and shouldn't render anymore.
     */

    protected boolean renderDisabled(ScreenRenderContext context){
       return false;
    }

    public boolean shouldRenderWhenDisabled(){
        return false;
    }

    public final void handleRender(ScreenRenderContext context){
        renderX = x;
        renderY = y;
        renderWidth = width;
        renderHeight = height;
        render(context);
    }

    protected void render(ScreenRenderContext context){

    }

    public void onWindowResized(int width, int height) {

    }

    public void onClick(ClickContext context) {

    }

    public UIComponent move(int x, int y) {
        this.x = x;
        this.y = y;

        return this;
    }

    public UIComponent move(int x, int y, int width, int height) {
        this.width = width;
        this.height = height;

        return move(x, y);
    }

    public void addChildren(UIComponent... children){
        for (UIComponent child : children) {
            if(child.parent != null) continue;

            this.children.add(child);
            child.parent = this;

            if(child.isEnabled()){
                enabledChildren.add(child);
            }
        }
    }

    public @Nullable UIComponent firstChild(){
        return children.isEmpty() ? null : children.getFirst();
    }

    public @Nullable UIComponent lastChild(){
        return children.isEmpty() ? null : children.getLast();
    }

    public UIComponent forEachImmediateChild(@NotNull Consumer<UIComponent> consumer){
        children.forEach(consumer);

        return this;
    }

    public boolean removeChild(UIComponent child){
        boolean removed = children.remove(child);
        enabledChildren.remove(child);
        if(removed) child.shutdown();

        return removed;
    }

    public void removeChildIf(Predicate<UIComponent> predicate){
        Iterator<UIComponent> iterator = children.iterator();
        while(iterator.hasNext()){
            UIComponent child = iterator.next();

            if(predicate.test(child)){
                child.shutdown();
                iterator.remove();
                enabledChildren.remove(child);
            }
        }
    }

    public UIComponent removeAllChildren(boolean recursivelyShutDown){
        if(recursivelyShutDown){
            children.forEach(UIComponent::shutdown);
        }

        children.clear();
        enabledChildren.clear();
        return this;
    }

    private void shutdown(){
        children.forEach(UIComponent::shutdown);
        onShutdown();
    }

    protected void onShutdown(){

    }

    public UIComponent topMostParent() {
        return topMostParent(false);
    }

    public UIComponent topMostParent(boolean ignoreBaseLayers) {
        if(parent == null){ //No parent, this IS the topmost parent
            return this;
        }else if(ignoreBaseLayers && parent instanceof InterfaceManager.UIBaseLayer) { //If ignoring base layers, and parent is base layer, this is the topmost parent.
            return this;
        }else {//Has parent, recursively go from there
            return parent.topMostParent(ignoreBaseLayers);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UIComponent that)) return false;
        return internalID == that.internalID;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(internalID);
    }

    protected void positionRelativeToParent(double dX, double dY){
        positionRelativeToParent(dX, dY, 0, 0);
    }

    protected void positionRelativeToParent(double dX, double dY, int paddingX, int paddingY){
        int referenceWidth = hasParent() ? parent.width : WindowUtil.scaledWidth();
        int referenceHeight = hasParent() ? parent.height : WindowUtil.scaledHeight();

        int x0 = hasParent() ? parent.x : 0;
        int y0 = hasParent() ? parent.y : 0;

        int w = referenceWidth - paddingX - this.width;
        x0 += paddingX + this.width / 2;

        int h = referenceHeight - paddingY - this.height;
        y0 += paddingY + this.height / 2;

        this.x = (int) (x0 + NumUtil.clamp(dX, 0.0, 1.0) * w - this.width / 2.0);
        this.y = (int) (y0 + NumUtil.clamp(dY, 0.0, 1.0) * h - this.height / 2.0);
    }

    protected void fill(ScreenRenderContext context, int color){
        context.fill(renderX, renderY, renderX + renderWidth, renderY + renderHeight, color);
    }

    protected void renderIcon(ScreenRenderContext context, Identifier identifier){
        renderIcon(context, identifier, 0xFFFFFFFF);
    }

    protected void renderIcon(ScreenRenderContext context, Identifier identifier, int color){
        context.drawGuiTexture(
                identifier,
                renderX,
                renderY,
                renderWidth,
                renderHeight,
                color
        );
    }

    public boolean isHoverable() {
        return true;
    }
}
