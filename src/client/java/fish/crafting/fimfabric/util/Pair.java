package fish.crafting.fimfabric.util;

/**
 * Minecraft's own pair type was dropped in 26.x, so FIM carries its own.
 * Same API as the old {@code net.minecraft.util.Pair} for the bits we used.
 */
public record Pair<A, B>(A left, B right) {

    public A getLeft() {
        return left;
    }

    public B getRight() {
        return right;
    }
}
