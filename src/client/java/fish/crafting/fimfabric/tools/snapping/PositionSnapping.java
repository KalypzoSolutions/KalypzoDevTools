package fish.crafting.fimfabric.tools.snapping;

import fish.crafting.fimfabric.util.render.CombinedFadeTracker;
import lombok.Getter;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract class PositionSnapping {

    private static final Function<Double, Double> SNAP_CENTER = pos -> (Mth.floor(pos)) + 0.5;
    private static final Function<Double, Double> SNAP_HALF = pos -> ((int) Math.round(pos / 0.5)) * 0.5;
    private static final Function<Double, Double> DONT_SNAP = pos -> pos;

    public static PositionSnapping
            HORIZONTAL_CENTER = PositionSnapping.of("Centered Y-Step", SNAP_CENTER, SNAP_HALF),
            BLOCK_CENTER = PositionSnapping.of("Centered Block", SNAP_CENTER, SNAP_CENTER),
            HALF_BLOCK = PositionSnapping.of("Half Block", SNAP_HALF, SNAP_HALF),
            FREE_MOVE = PositionSnapping.of("Free Move", DONT_SNAP, DONT_SNAP);

    public static final PositionSnapping[] ALL_SNAPPINGS = {HORIZONTAL_CENTER, BLOCK_CENTER, HALF_BLOCK, FREE_MOVE};
    public final CombinedFadeTracker fade = new CombinedFadeTracker(0.2);
    @NotNull @Getter
    private final String name;

    public PositionSnapping(@NotNull String name) {
        this.name = name;
    }

    public abstract double snapXZ(double pos);
    public abstract double snapY(double pos);

    public Vec3 snap(Vec3 original){
        return new Vec3(
                snapXZ(original.x),
                snapY(original.y),
                snapXZ(original.z)
        );
    }

    private static PositionSnapping of(@NotNull String name,
                                       @NotNull Function<Double, Double> horizontal,
                                       @NotNull Function<Double, Double> vertical){
        return new PositionSnapping(name) {
            @Override
            public double snapXZ(double pos) {
                return horizontal.apply(pos);
            }

            @Override
            public double snapY(double pos) {
                return vertical.apply(pos);
            }
        };
    }

}
