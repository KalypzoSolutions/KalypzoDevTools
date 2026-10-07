package fish.crafting.fimfabric.tools.render;

import fish.crafting.fimfabric.util.VectorUtils;
import fish.crafting.fimfabric.util.Pair;
import net.minecraft.world.phys.Vec3;

public enum ToolAxis {

    X(0xFFFF0000, new Vec3(1, 0, 0)),
    Y(0xFF00FF00, new Vec3(0, 1, 0)),
    Z(0xFF0000FF, new Vec3(0, 0, 1));

    public final int commonColor;
    public final Vec3 unit;
    public final Vec3 oppositeUnit;
    public final Vec3[] planeNormals;

    ToolAxis(int commonColor, Vec3 unit){
        this.commonColor = commonColor;
        this.unit = unit;
        this.oppositeUnit = new Vec3(1.0 - unit.x, 1.0 - unit.y, 1.0 - unit.z);
        this.planeNormals = new Vec3[2];
        this.planeNormals[0] = VectorUtils.moveCoordsOnePositionOver(unit);
        this.planeNormals[1] = VectorUtils.moveCoordsOnePositionOver(this.planeNormals[0]);
    }

    /**
     * Makes a plane vector from a normal, using coordinate1 and coordinate2
     */
    public Vec3 makePlaneFromNormal(double coordinate1, double coordinate2) {
        return VectorUtils.multiplyNonZerosInOrder(oppositeUnit, coordinate1, coordinate2);
    }

    public Pair<Vec3, Vec3> getNonUnitDirections(){
        if(unit.x != 0.0) return new Pair<>(new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0));
        if(unit.y != 0.0) return new Pair<>(new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0));
        return new Pair<>(new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0));
    }

    public int color(ToolAxis currentlyHovered){
        return currentlyHovered != this ? commonColor : (commonColor | 0xFFAAAAAA);
    }

    public double getValueFromUnit(Vec3 vec3d){
        return VectorUtils.getValueUsingUnitVector(vec3d, unit);
    }
}
