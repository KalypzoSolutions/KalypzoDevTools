package fish.crafting.fimfabric.util;

import fish.crafting.fimfabric.connection.ConnectionManager;
import fish.crafting.fimfabric.mixin.WorldAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class LocationUtils {

    public static String getClientWorldName(){
        ClientWorld world = MinecraftClient.getInstance().world;
        if(world == null) return null;

        WorldAccessor accessor = (WorldAccessor) world;
        RegistryKey<World> registryKey = accessor.getRegistryKey();
        if(registryKey == null) return null;

        Identifier identifier = registryKey.getValue();
        if(identifier.getNamespace().equals("minecraft")) return identifier.getPath();

        //modded:world_name
        return identifier.toString();
    }

    public static String toGenericCoords(Vec3d vec3d) {
        return toGeneric(VectorUtils.toCoordsString(vec3d));
    }

    public static String toGenericLocation(Vec3d vec3d, float pitch, float yaw) {
        return toGeneric(toLocationCoords(vec3d, pitch, yaw));
    }

    private static String toGeneric(String content){
        if(ConnectionManager.kotlin){
            return "Location(world, " + content + ")";
        }else{
            return "new Location(world, " + content + ");";
        }
    }

    public static String toCoordsString(Vec3d vec3d){
        String worldName = getClientWorldName();
        if(worldName == null) return null;

        String s = VectorUtils.toCoordsString(vec3d);
        return wrap(s, worldName);
    }

    public static String toLocationString(Vec3d vec3d, float pitch, float yaw){
        String worldName = getClientWorldName();
        if(worldName == null) return null;

        String s = toLocationCoords(vec3d, pitch, yaw);
        return wrap(s, worldName);
    }

    private static String toLocationCoords(Vec3d vec3d, float pitch, float yaw){
        return NumUtil.toCodeNumber(vec3d.x, true) + ", "
                + NumUtil.toCodeNumber(vec3d.y, true) + ", "
                + NumUtil.toCodeNumber(vec3d.z, true) + ", "
                + NumUtil.toCodeNumber(yaw, true) + "f, "
                + NumUtil.toCodeNumber(pitch, true) + "f";
    }

    private static String wrap(String coords, @NotNull String worldName) {
        if(ConnectionManager.kotlin){
            return "Location(Bukkit.getWorld(\"" + worldName + "\"), " + coords + ")";
        }else{
            return "new Location(Bukkit.getWorld(\"" + worldName + "\"), " + coords + ");";
        }
    }
}
