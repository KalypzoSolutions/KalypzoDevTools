package fish.crafting.fimfabric.keybind;

import fish.crafting.fimfabric.client.FIMModClient;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

// Categories are registered Category records identified by an Identifier. Their label is resolved
// as `key.category.<namespace>.<path>`.
public abstract class CustomKeybind {

    private static final Map<KeybindCategory, KeyMapping.Category> CATEGORIES = new EnumMap<>(KeybindCategory.class);

    public final KeyMapping keyBinding;

    public CustomKeybind(@NotNull String keyID,
                         @NotNull InputConstants.Type type, int code,
                         @NotNull KeybindCategory category){
        this.keyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "keybind.fim." + keyID,
                type,
                code,
                category(category)
        ));
    }

    private static KeyMapping.Category category(@NotNull KeybindCategory category){
        return CATEGORIES.computeIfAbsent(category, c ->
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(FIMModClient.NAMESPACE, c.id())));
    }

    public abstract void onPressed();

}
