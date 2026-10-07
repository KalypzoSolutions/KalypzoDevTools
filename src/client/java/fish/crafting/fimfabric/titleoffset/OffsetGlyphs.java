package fish.crafting.fimfabric.titleoffset;

/**
 * The characters CraftEngine emits for {@code <shift:N>}. Two blocks of 21 characters each, one
 * shifting left and one shifting right, that are summed up to reach the requested amount.
 */
public class OffsetGlyphs {

    private static final char NEGATIVE_BASE = 0xF800;
    private static final char POSITIVE_BASE = 0xF830;
    //Position inside either block -> pixels shifted
    private static final int[] AMOUNTS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 32, 48, 64, 128, 256};

    public static boolean isOffset(char c){
        return valueOf(c) != 0;
    }

    public static int valueOf(char c){
        if(c >= NEGATIVE_BASE && c < NEGATIVE_BASE + AMOUNTS.length) return -AMOUNTS[c - NEGATIVE_BASE];
        if(c >= POSITIVE_BASE && c < POSITIVE_BASE + AMOUNTS.length) return AMOUNTS[c - POSITIVE_BASE];
        return 0;
    }

    public static int decode(String run){
        int shift = 0;
        for(int i = 0; i < run.length(); i++){
            shift += valueOf(run.charAt(i));
        }
        return shift;
    }

    /**
     * Largest amount first, the same way the server does it: -40 becomes -32 and -8.
     */
    public static String encode(int shift){
        char base = shift < 0 ? NEGATIVE_BASE : POSITIVE_BASE;
        int remaining = Math.abs(shift);

        StringBuilder run = new StringBuilder();
        int i = AMOUNTS.length - 1;
        while(remaining > 0){
            if(AMOUNTS[i] <= remaining){
                run.append((char) (base + i));
                remaining -= AMOUNTS[i];
            }else{
                i--;
            }
        }
        return run.toString();
    }

}
