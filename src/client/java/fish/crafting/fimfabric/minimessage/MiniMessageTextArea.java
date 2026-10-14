package fish.crafting.fimfabric.minimessage;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import fish.crafting.fimfabric.mixin.MultilineTextFieldAccessor;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.KeyEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A text area made for MiniMessage: undo and redo, formatting of the selection and shortcuts.
 * It does not know where it is used, so it works in any owo screen.
 *
 * <ul>
 *   <li>Ctrl+Z undo, Ctrl+Y or Ctrl+Shift+Z redo</li>
 *   <li>Ctrl+B bold, Ctrl+I italic, Ctrl+U underlined</li>
 *   <li>{@link #format} wraps the selection in a tag, and takes it out again if it is already there</li>
 * </ul>
 */
public class MiniMessageTextArea extends TextAreaComponent {

    //Typing within this time ends up in one undo step
    private static final long MERGE_MILLIS = 800;
    private static final int HISTORY_LIMIT = 300;

    private record Snapshot(String value, int cursor) {}

    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private final Deque<Snapshot> redoStack = new ArrayDeque<>();

    private String lastValue;
    private int lastCursor;
    private long lastEdit = 0;
    private boolean restoring = false;
    //The next change starts a step of its own, used before programmatic edits
    private boolean breakNext = false;

    public MiniMessageTextArea(@NotNull Sizing horizontal, @NotNull Sizing vertical, @NotNull String initial) {
        super(horizontal, vertical);

        text(initial);
        lastValue = getValue();
        lastCursor = editBox.cursor();

        onChanged().subscribe(this::changed);
    }

    // ------------------------------------------------------------------ history

    private void changed(String value){
        if(restoring || value.equals(lastValue)) return;

        long now = System.currentTimeMillis();
        //Typing one character at a time merges, pasting or deleting a selection does not
        boolean single = Math.abs(value.length() - lastValue.length()) == 1;
        if(undoStack.isEmpty() || breakNext || !single || now - lastEdit > MERGE_MILLIS){
            undoStack.push(new Snapshot(lastValue, lastCursor));
            while(undoStack.size() > HISTORY_LIMIT) undoStack.removeLast();
        }

        breakNext = false;
        redoStack.clear();
        lastEdit = now;
        lastValue = value;
        lastCursor = editBox.cursor();
    }

    public boolean undo(){
        if(undoStack.isEmpty()) return false;

        redoStack.push(new Snapshot(getValue(), editBox.cursor()));
        restore(undoStack.pop());
        return true;
    }

    public boolean redo(){
        if(redoStack.isEmpty()) return false;

        undoStack.push(new Snapshot(getValue(), editBox.cursor()));
        restore(redoStack.pop());
        return true;
    }

    private void restore(Snapshot snapshot){
        restoring = true;
        try {
            setValue(snapshot.value());
            editBox.seekCursor(Whence.ABSOLUTE, Math.min(snapshot.cursor(), snapshot.value().length()));
        } finally {
            restoring = false;
        }

        lastValue = snapshot.value();
        lastCursor = editBox.cursor();
        breakNext = true;
    }

    public boolean canUndo(){
        return !undoStack.isEmpty();
    }

    public boolean canRedo(){
        return !redoStack.isEmpty();
    }

    // ------------------------------------------------------------------ editing

    /**
     * Replaces the whole text as one undo step.
     */
    public void replaceAll(@NotNull String value){
        if(value.equals(getValue())) return;

        breakNext = true;
        text(value);
    }

    /**
     * Puts text at the cursor. A selection is kept and the text goes behind it.
     */
    public void insertAtCursor(@NotNull String text){
        breakNext = true;
        if(editBox.hasSelection()){
            int end = selectionEnd();
            select(end, end);
        }
        editBox.insertText(text);
    }

    /**
     * Wraps the selection in {@code open} and {@code close}. If it already is wrapped like that, the
     * tags come off again. Without a selection both tags are put in with the cursor between them.
     */
    public void format(@NotNull String open, @NotNull String close){
        breakNext = true;

        String value = getValue();
        int begin = selectionBegin();
        int end = selectionEnd();

        if(begin == end){
            editBox.insertText(open + close);
            editBox.seekCursor(Whence.RELATIVE, -close.length());
            return;
        }

        String inner = value.substring(begin, end);

        //The tags are part of the selection
        if(inner.length() >= open.length() + close.length() && inner.startsWith(open) && inner.endsWith(close)){
            replaceSelection(begin, end, inner.substring(open.length(), inner.length() - close.length()));
            return;
        }

        //The tags sit right around the selection
        if(begin >= open.length() && value.startsWith(open, begin - open.length())
                && value.startsWith(close, end)){
            replaceSelection(begin - open.length(), end + close.length(), inner);
            return;
        }

        replaceSelection(begin, end, open + inner + close);
        //Select what was selected before, so the next tag stacks on top of this one
        select(begin + open.length(), begin + open.length() + inner.length());
    }

    /**
     * Takes every tag out of the selection.
     */
    public void stripSelection(){
        if(!editBox.hasSelection()) return;

        breakNext = true;
        int begin = selectionBegin();
        String stripped = editBox.getSelectedText().replaceAll("<[^<>]*>", "");
        editBox.insertText(stripped);
        select(begin, begin + stripped.length());
    }

    private void replaceSelection(int begin, int end, String replacement){
        select(begin, end);
        editBox.insertText(replacement);
    }

    private void select(int from, int to){
        editBox.setSelecting(false);
        editBox.seekCursor(Whence.ABSOLUTE, from);
        editBox.setSelecting(true);
        editBox.seekCursor(Whence.ABSOLUTE, to);
        editBox.setSelecting(false);
    }

    //Cursor and selection anchor, the selection runs between them in either direction
    private int selectionBegin(){
        return Math.min(editBox.cursor(), ((MultilineTextFieldAccessor) editBox).getFimSelectCursor());
    }

    private int selectionEnd(){
        return Math.max(editBox.cursor(), ((MultilineTextFieldAccessor) editBox).getFimSelectCursor());
    }

    public @Nullable String selectedText(){
        return editBox.hasSelection() ? editBox.getSelectedText() : null;
    }

    /**
     * Gives the keyboard back to the text, for buttons that took it away by being clicked.
     */
    public void refocus(){
        var handler = focusHandler();
        if(handler != null) handler.focus(this, UIComponent.FocusSource.MOUSE_CLICK);
    }

    // ------------------------------------------------------------------ keys

    @Override
    public boolean keyPressed(KeyEvent event) {
        if(event.hasControlDown() && !event.hasAltDown()){
            switch (event.key()) {
                case InputConstants.KEY_Z -> {
                    if(event.hasShiftDown()) redo();
                    else undo();
                    return true;
                }
                case InputConstants.KEY_Y -> {
                    redo();
                    return true;
                }
                case InputConstants.KEY_B -> {
                    format("<bold>", "</bold>");
                    return true;
                }
                case InputConstants.KEY_I -> {
                    format("<italic>", "</italic>");
                    return true;
                }
                case InputConstants.KEY_U -> {
                    format("<underlined>", "</underlined>");
                    return true;
                }
                default -> {}
            }
        }

        return super.keyPressed(event);
    }

    /**
     * For code that needs the raw editing state, like a custom toolbar.
     */
    protected MultilineTextField field(){
        return editBox;
    }

}
