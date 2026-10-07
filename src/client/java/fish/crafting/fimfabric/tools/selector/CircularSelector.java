package fish.crafting.fimfabric.tools.selector;

import fish.crafting.fimfabric.util.VectorUtils;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

//NOTE: Make sure the returned BOX is of same width and length, circle-wise!
public abstract class CircularSelector extends WorldSelector{

    private final Vec3 normal;
    private final double outerRadiusPercent;
    protected double maxRadiusExtend = 0.0;

    /**
     * @param outerRadiusPercent Percent of the outer circle radius cut
     */
    public CircularSelector(Vec3 normal, double outerRadiusPercent){
        this.normal = normal;
        this.outerRadiusPercent = outerRadiusPercent;
    }

    @Override
    public @Nullable Vec3 raycast(Vec3 from, Vec3 to) {
        Vec3 raycast = super.raycast(from, to);
        if(raycast == null) return null;

        AABB box = getBox();
        Vec3 invertedNormal = VectorUtils.invertZeros(normal);

        Vec3 center = box.getCenter().multiply(invertedNormal);
        Vec3 point = raycast.multiply(invertedNormal);

        double sqD = center.distanceToSqr(point);
        double maxR = (normal.y > 0.0 ? box.getXsize() : box.getYsize()) / 2.0;
        double minR = outerRadiusPercent * maxR;

        maxR += maxRadiusExtend;

        maxR *= maxR; //square
        minR *= minR; //yup

        if(sqD <= maxR && sqD >= minR) return raycast;
        return null;
    }
}
