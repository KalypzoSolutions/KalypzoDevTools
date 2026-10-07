package fish.crafting.fimfabric.tools;

import net.minecraft.world.phys.Vec3;

public interface PosScaled extends Positioned{

    double scaleX();
    double scaleY();
    double scaleZ();

    void setScale(double x, double y, double z);

    default Vec3 scaleVec(){
        return new Vec3(scaleX(), scaleY(), scaleZ());
    }
}
