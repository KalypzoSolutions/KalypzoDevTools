package fish.crafting.fimfabric.inspect.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Everything the inspector shows about an item or a block, collected up front so the screen only
 * has to lay it out.
 *
 * @param title What the screen is called.
 * @param icon The item that stands for it.
 * @param editable The stack the editor changes, null for blocks.
 */
public record Inspection(@NotNull String title, @NotNull ItemStack icon, @Nullable ItemStack editable, @NotNull List<Row> rows) {

    /**
     * @param heading A line that only separates sections.
     * @param copy What the copy button puts into the clipboard, null for none.
     */
    public record Row(@NotNull String key, @NotNull String value, boolean heading, @Nullable String copy) {}

    public static Inspection ofItem(@NotNull ItemStack stack){
        List<Row> rows = new ArrayList<>();
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        rows.add(new Row("ID", id, false, id));
        rows.add(new Row("Count", stack.getCount() + " / " + stack.getMaxStackSize(), false, null));
        if(stack.isDamageableItem()){
            rows.add(new Row("Durability", (stack.getMaxDamage() - stack.getDamageValue()) + " / " + stack.getMaxDamage(), false, null));
        }
        String name = stack.getHoverName().getString();
        rows.add(new Row("Name", name, false, name));

        rows.add(heading("Components"));
        List<TypedDataComponent<?>> components = new ArrayList<>();
        stack.getComponents().forEach(components::add);
        components.sort((a, b) -> key(a).compareTo(key(b)));
        for (TypedDataComponent<?> component : components) {
            String value = component.encodeValue(NbtOps.INSTANCE).result()
                    .map(Object::toString).orElse(String.valueOf(component.value()));
            boolean custom = !java.util.Objects.equals(stack.getPrototype().get(component.type()), component.value());
            rows.add(new Row(key(component) + (custom ? "" : "  (default)"), value, false, value));
        }

        rows.add(heading("Tags"));
        for (String tag : tags(stack.typeHolder())) rows.add(new Row("", tag, false, tag));

        return new Inspection(id, stack, stack, rows);
    }

    public static Inspection ofBlock(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state){
        List<Row> rows = new ArrayList<>();
        Block block = state.getBlock();
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        String position = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        String name = block.getName().getString();

        List<String> properties = new ArrayList<>();
        List<Row> propertyRows = new ArrayList<>();
        for (Property<?> property : state.getProperties()) {
            String value = value(property, state);
            properties.add(property.getName() + "=" + value);
            propertyRows.add(new Row(property.getName(), value, false, value));
        }
        String full = properties.isEmpty() ? id : id + "[" + String.join(",", properties) + "]";

        rows.add(new Row("ID", id, false, id));
        rows.add(new Row("State", full, false, full));
        rows.add(new Row("Position", position, false, position));
        rows.add(new Row("Name", name, false, name));
        rows.add(new Row("Hardness", String.valueOf(state.getDestroySpeed(level, pos)), false, null));
        rows.add(new Row("Light", String.valueOf(state.getLightEmission()), false, null));
        rows.add(new Row("Item", BuiltInRegistries.ITEM.getKey(block.asItem()).toString(), false, null));

        if(!propertyRows.isEmpty()){
            rows.add(heading("Block state"));
            rows.addAll(propertyRows);
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if(entity != null){
            rows.add(heading("Block entity"));
            rows.add(new Row("Type", String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType())), false, null));
        }

        rows.add(heading("Tags"));
        for (String tag : tags(block.builtInRegistryHolder())) rows.add(new Row("", tag, false, tag));

        return new Inspection(id, new ItemStack(block.asItem()), null, rows);
    }

    private static <T extends Comparable<T>> String value(Property<T> property, BlockState state){
        return property.getName(state.getValue(property));
    }

    private static String key(TypedDataComponent<?> component){
        var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
        return key == null ? component.type().toString() : key.toString();
    }

    private static <T> List<String> tags(Holder<T> holder){
        return holder.tags().map(TagKey::location).map(Object::toString).sorted().collect(Collectors.toList());
    }

    private static Row heading(String text){
        return new Row(text, "", true, null);
    }

}
