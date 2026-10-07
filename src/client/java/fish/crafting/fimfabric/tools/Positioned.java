package fish.crafting.fimfabric.tools;

import fish.crafting.fimfabric.tools.selector.WorldSelector;
import net.minecraft.world.phys.Vec3;

public interface Positioned {

    WorldSelector selector();
    Vec3 getPos();
    void setPos(double x, double y, double z);

}
