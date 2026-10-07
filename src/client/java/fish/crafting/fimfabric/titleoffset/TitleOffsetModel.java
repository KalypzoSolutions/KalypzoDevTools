package fish.crafting.fimfabric.titleoffset;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A container title taken apart into its shifts and everything in between. Only the shifts can be
 * changed, the rest is put back exactly as the server sent it.
 */
public class TitleOffsetModel {

    private final List<Part> parts = new ArrayList<>();
    @Getter
    private final List<Slot> slots = new ArrayList<>();

    public TitleOffsetModel(Component title){
        //Styles arrive already resolved against their parents, so the parts can be rebuilt flat
        title.visit((style, text) -> {
            split(style, text);
            return Optional.empty();
        }, Style.EMPTY);

        labelSlots();
    }

    private void split(Style style, String text){
        //The same code points can be image glyphs in any other font
        if(!FontDescription.DEFAULT.equals(style.getFont())){
            parts.add(new Part(style, text, null));
            return;
        }

        int start = 0;
        while(start < text.length()){
            boolean offset = OffsetGlyphs.isOffset(text.charAt(start));
            int end = start + 1;
            while(end < text.length() && OffsetGlyphs.isOffset(text.charAt(end)) == offset) end++;

            String run = text.substring(start, end);
            if(offset){
                Slot slot = new Slot(slots.size(), OffsetGlyphs.decode(run));
                slots.add(slot);
                parts.add(new Part(style, run, slot));
            }else{
                parts.add(new Part(style, run, null));
            }
            start = end;
        }
    }

    /**
     * Names every shift after the text it moves, which is whatever follows until the next shift.
     */
    private void labelSlots(){
        Slot current = null;
        StringBuilder following = new StringBuilder();
        boolean hasGlyphs = false;

        for(Part part : parts){
            if(part.slot != null){
                if(current != null) current.label = labelOf(following, hasGlyphs);
                current = part.slot;
                following.setLength(0);
                hasGlyphs = false;
                continue;
            }

            for(int i = 0; i < part.text.length(); i++){
                char c = part.text.charAt(i);
                //Private use area, i.e. images
                if(c >= 0xE000 && c <= 0xF8FF){
                    hasGlyphs = true;
                }else{
                    following.append(c);
                }
            }
        }

        if(current != null) current.label = labelOf(following, hasGlyphs);
    }

    private static String labelOf(StringBuilder following, boolean hasGlyphs){
        String label = following.toString().trim();
        if(label.isEmpty() && hasGlyphs) return "[glyph]";
        return label;
    }

    public Component build(){
        MutableComponent title = Component.empty();
        for(Part part : parts){
            String text = part.slot == null ? part.text : OffsetGlyphs.encode(part.slot.value);
            if(text.isEmpty()) continue;

            title.append(Component.literal(text).setStyle(part.style));
        }
        return title;
    }

    /**
     * One line per shift, in the notation used on the server.
     */
    public String export(){
        StringBuilder export = new StringBuilder();
        for(Slot slot : slots){
            export.append("<shift:").append(slot.value).append(">");
            if(!slot.label.isEmpty()) export.append("  ").append(slot.label);
            export.append("\n");
        }
        return export.toString();
    }

    public void reset(){
        for(Slot slot : slots){
            slot.value = slot.original;
        }
    }

    public boolean isModified(){
        for(Slot slot : slots){
            if(slot.value != slot.original) return true;
        }
        return false;
    }

    /**
     * Whether the other title was sent with the same shifts, in which case it is the same GUI opened again.
     */
    public boolean hasSameOriginals(TitleOffsetModel other){
        if(slots.size() != other.slots.size()) return false;

        for(int i = 0; i < slots.size(); i++){
            if(slots.get(i).original != other.slots.get(i).original) return false;
        }
        return true;
    }

    public void copyValuesFrom(TitleOffsetModel other){
        for(int i = 0; i < slots.size(); i++){
            slots.get(i).value = other.slots.get(i).value;
        }
    }

    private record Part(Style style, String text, @Nullable Slot slot) {}

    @Getter
    public static class Slot {
        private final int index;
        private final int original;
        @Setter
        private int value;
        private String label = "";

        private Slot(int index, int original){
            this.index = index;
            this.original = original;
            this.value = original;
        }
    }

}
