package fish.crafting.fimfabric.util;

/**
 * Minecraft has no pair type of its own, so FIM carries one.
 */
public record Pair<A, B>(A left, B right) {

    public A getLeft() {
        return left;
    }

    public B getRight() {
        return right;
    }
}
