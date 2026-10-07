package fish.crafting.fimfabric.ui.custom.quickactions;

import fish.crafting.fimfabric.ui.FancyText;
import fish.crafting.fimfabric.ui.actions.ActionElement;
import fish.crafting.fimfabric.ui.actions.BlockPosActionElement;
import fish.crafting.fimfabric.ui.actions.UIActionList;
import fish.crafting.fimfabric.util.ActionUtils;
import fish.crafting.fimfabric.util.ClickContext;
import fish.crafting.fimfabric.util.LocationUtils;
import fish.crafting.fimfabric.util.VectorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class LocationActions extends UIActionList {

    public LocationActions(int width) {
        super(width);

        addElement(new CopyPlayerLocation());
        addElement(new CopyPlayerLocationGeneric());
        addSeparator();
        addElement(new CopyTargetBlockLocation());
        addElement(new CopyTargetBlockLocationGeneric());
    }

    public static class CopyPlayerLocation extends ActionElement {
        public CopyPlayerLocation() {
            super(FancyText.of("Copy Player Location"));
        }

        @Override
        protected void activate(ClickContext context) {
            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if(player == null) return;

            ActionUtils.copyIfClick(context, () -> LocationUtils.toLocationString(player.position(), player.getXRot(), player.getYRot()));
        }
    }
    public static class CopyPlayerLocationGeneric extends ActionElement {
        public CopyPlayerLocationGeneric() {
            super(FancyText.of("Copy Player Location (Generic)"));
        }

        @Override
        protected void activate(ClickContext context) {
            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if(player == null) return;

            ActionUtils.copyIfClick(context, () -> LocationUtils.toGenericLocation(player.position(), player.getXRot(), player.getYRot()));
        }
    }

    public static class CopyTargetBlockLocation extends BlockPosActionElement {

        public CopyTargetBlockLocation() {
            super(FancyText.of("Copy Target-Block Location"));
        }

        @Override
        protected void activate(ClickContext context, BlockPos pos) {
            ActionUtils.copyIfClick(context, () -> LocationUtils.toCoordsString(Vec3.atLowerCornerOf(pos)));
        }
    }

    public static class CopyTargetBlockLocationGeneric extends BlockPosActionElement {

        public CopyTargetBlockLocationGeneric() {
            super(FancyText.of("Copy Target-Block Location (Generic)"));
        }

        @Override
        protected void activate(ClickContext context, BlockPos pos) {
            ActionUtils.copyIfClick(context, () -> LocationUtils.toGenericCoords(Vec3.atLowerCornerOf(pos)));
        }
    }
}