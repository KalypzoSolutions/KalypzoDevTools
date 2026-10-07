package fish.crafting.fimfabric.util;

import com.google.common.primitives.Doubles;
import fish.crafting.fimfabric.connection.ConnectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class VectorUtils {

    public static Vec3 lineIntersection(Vec3 planePoint, Vec3 planeNormal, Vec3 linePoint, Vec3 lineDirection) {
        if (planeNormal.dot(lineDirection.normalize()) == 0) {
            return null;
        }

        double t = (planeNormal.dot(planePoint) - planeNormal.dot(linePoint)) / planeNormal.dot(lineDirection.normalize());
        return linePoint.add(lineDirection.normalize().scale(t));
    }

    public static Vec3 moveCoordsOnePositionOver(Vec3 vec3d){
        return new Vec3(
                vec3d.z,
                vec3d.x,
                vec3d.y
        );
    }

    public static double angle(Vec3 first, Vec3 other){
        double dot = Doubles.constrainToRange(first.dot(other) / (first.length() * other.length()), -1.0, 1.0);
        return (float) Math.acos(dot);
    }

    public static Vec3 getDirection(float pitch, float yaw){
        float f = pitch * ((float)Math.PI / 180);
        float g = -yaw * ((float)Math.PI / 180);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    public static float pitchFromDirection(Vec3 vec3d){
        float k = (float) -vec3d.y;
        float f = (float) Math.asin(k);
        float pitch = f / ((float)Math.PI / 180);
        return vec3d.z < 0 ? 180f - pitch : pitch;
    }

    public static float yawFromDirection(Vec3 vec3d){
        float i = (float) vec3d.x;
        float g = (float) Math.asin(i);
        float yaw = -g / ((float)Math.PI / 180);
        return vec3d.z < 0 ? 180f - yaw : yaw;
    }

    public static double sumCoords(Vec3 vec3d) {
        return vec3d.x + vec3d.y + vec3d.z;
    }

    public static double getValueUsingUnitVector(Vec3 target, Vec3 unit) {
        if(unit.x != 0.0)      return target.x;
        else if(unit.y != 0.0) return target.y;
        else                   return target.z;
    }

    /**
     * Converts all 0 values to 1, and all non-0 values to 0
     */
    public static Vec3 invertZeros(Vec3 vec3d) {
        double x = vec3d.x == 0.0 ? 1.0 : 0.0;
        double y = vec3d.y == 0.0 ? 1.0 : 0.0;
        double z = vec3d.z == 0.0 ? 1.0 : 0.0;
        return new Vec3(x, y, z);
    }

    /**
     * Multiplies non-zero values, each by a different number from the array, in order.
     */
    public static Vec3 multiplyNonZerosInOrder(Vec3 vec3d, double... values){
        int l = values.length;
        int i = 0;
        double x = 0.0, y = 0.0, z = 0.0;
        if(vec3d.x != 0.0) x = vec3d.x * values[i++ % l];
        if(vec3d.y != 0.0) y = vec3d.y * values[i++ % l];
        if(vec3d.z != 0.0) z = vec3d.z * values[i % l];
        return new Vec3(x, y, z);
    }

    public static String toCoordsString(Vec3 vec3d){
        return NumUtil.toCodeNumber(vec3d.x, true) + ", "
                + NumUtil.toCodeNumber(vec3d.y, true) + ", "
                + NumUtil.toCodeNumber(vec3d.z, true);
    }

    public static String toString(Vec3 vec3d){
        String s = toCoordsString(vec3d);
        if(ConnectionManager.kotlin){
            return "Vector(" + s + ")";
        }else{
            return "new Vector(" + s + ");";
        }
    }

    public static String toString(BlockPos pos){
        return toString(new Vec3(pos));
    }

    public static Vec3 toBlockPos(Vec3 vec3d) {
        return new Vec3(
                Mth.floor(vec3d.x),
                Mth.floor(vec3d.y),
                Mth.floor(vec3d.z)
        );
    }
}
