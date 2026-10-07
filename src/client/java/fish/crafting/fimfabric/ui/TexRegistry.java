package fish.crafting.fimfabric.ui;

import fish.crafting.fimfabric.client.FIMModClient;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class TexRegistry {

    public static final Identifier
    UI_BURGER = ui("burger"),
    TOOL_MOVE = ui("tools/move"),
    TOOL_ROTATE = ui("tools/rotate"),
    TOOL_SCALE = ui("tools/scale"),
    VECTOR = ui("vector"),
    LOCATION = ui("location"),
    BOUNDING_BOX = ui("boundingbox"),
    TELEPORT = ui("teleport"),
    COORDINATES = ui("coordinates"),
    FEED_QUESTION = ui("feed/question"),
    FEED_WARN = ui("feed/warn"),
    FEED_ERROR = ui("feed/error"),
    FEED_SUCCESS = ui("feed/success"),
    SMALL_X = ui("small_x")
    ;


    private static @NotNull Identifier widget(@NotNull String name){
        return Identifier.fromNamespaceAndPath(FIMModClient.NAMESPACE, "widget/" + name);
    }

    private static @NotNull Identifier ui(@NotNull String name){
        return widget("ui/" + name);
    }

}
