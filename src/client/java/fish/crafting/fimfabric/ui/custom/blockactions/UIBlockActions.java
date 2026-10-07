package fish.crafting.fimfabric.ui.custom.blockactions;

import fish.crafting.fimfabric.ui.FancyText;
import fish.crafting.fimfabric.ui.actions.UIActionList;
import fish.crafting.fimfabric.util.ActionUtils;
import fish.crafting.fimfabric.util.CursorPicking;
import fish.crafting.fimfabric.util.LocationUtils;
import fish.crafting.fimfabric.util.VectorUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class UIBlockActions extends UIActionList {

    public static Vec3 storedRayPos = Vec3.ZERO;
    public static BlockPos storedRayBlockPos = new BlockPos(0, 0, 0);

    public UIBlockActions() {
        super(WIDTH_MEDIUM);

        title(FancyText.of("Block Actions"));
        addRunElement(FancyText.vector("Copy Center Vector"), ctx -> {
            Vec3 pos = Vec3.atCenterOf(storedRayBlockPos);
            ActionUtils.copyIfClick(ctx, () -> VectorUtils.toString(pos));
        });

        addRunElement(FancyText.vector("Copy Corner Vector"), ctx -> {
            ActionUtils.copyIfClick(ctx, () -> VectorUtils.toString(storedRayBlockPos));
        });

        addSeparator();

        addRunElement(FancyText.coords("Copy Center Coordinates"), ctx -> {
            Vec3 pos = Vec3.atCenterOf(storedRayBlockPos);
            ActionUtils.copyIfClick(ctx, () -> VectorUtils.toCoordsString(pos));
        });

        addRunElement(FancyText.coords("Copy Corner Coordinates"), ctx -> {
            Vec3 pos = new Vec3(storedRayBlockPos);
            ActionUtils.copyIfClick(ctx, () -> VectorUtils.toCoordsString(pos));
        });

        addSeparator();

        addRunElement(FancyText.location("Copy Center Location"), ctx -> {
            Vec3 pos = Vec3.atCenterOf(storedRayBlockPos);
            ActionUtils.copyIfClick(ctx, () -> LocationUtils.toCoordsString(pos));
        });

        addRunElement(FancyText.location("Copy Corner Location"), ctx -> {
            Vec3 pos = new Vec3(storedRayBlockPos);
            ActionUtils.copyIfClick(ctx, () -> LocationUtils.toCoordsString(pos));
        });

        addSeparator();

        addRunElement(FancyText.of("Copy BlockState"), ctx -> {
            if(!ctx.isLeftClickPress()) return;

            Minecraft instance = Minecraft.getInstance();
            ClientLevel world = instance.level;
            if(world == null) return;

            BlockState state = world.getBlockState(storedRayBlockPos);
            if(state == null) return;

            String str = state.toString();
            if(str.startsWith("Block{")) {
                str = str.substring(6).replaceFirst("}", "");
            }

            if(state.getValues().findAny().isPresent()){ //okay now we do some freaky shit to not include default properties
                BlockState defaultState = state.getBlock().defaultBlockState();

                String defaultStateStr = defaultState.toString();
                String[] split = defaultStateStr.split(",");
                if(split.length > 0) {
                    //Remove the first part: 'minecraft:stone['
                    int index = split[0].lastIndexOf("[");
                    if(index != -1 && index < split[0].length() - 1) split[0] = split[0].substring(index + 1);

                    //Remove last ]
                    split[split.length - 1] = split[split.length - 1].replace("]", "");
                }

                for (String s : split) {
                    //Painful but the strings are short and this isn't done often
                    if(str.contains("," + s)) {
                        str = str.replace("," + s, "");
                    }else{
                        str = str.replace(s + ",", "");
                    }

                    str = str.replace(s, "");
                }

                if(str.endsWith("[]")) str = str.substring(0, str.length() - 2);
            }

            String finalStr = str;
            ActionUtils.copyIfClick(ctx, () -> finalStr);

        });
    }

    @Override
    protected void onEnable(boolean wasDisabled) {
        super.onEnable(wasDisabled);
        BlockHitResult raycast = CursorPicking.raycast();
        if(raycast == null) return;

        Vec3 pos = raycast.getLocation();
        if(pos != null) {
            storedRayPos = pos;
        }

        BlockPos blockPos = raycast.getBlockPos();
        if(pos != null) {
            storedRayBlockPos = blockPos;
        }
    }
}
