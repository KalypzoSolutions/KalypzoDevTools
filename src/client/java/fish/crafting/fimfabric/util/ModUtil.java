package fish.crafting.fimfabric.util;

import fish.crafting.fimfabric.client.FIMModClient;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class ModUtil {


    public static Identifier identifier(@NotNull String id){
        return Identifier.fromNamespaceAndPath(FIMModClient.NAMESPACE, id);
    }

}
