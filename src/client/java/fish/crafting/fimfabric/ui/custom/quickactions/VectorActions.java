package fish.crafting.fimfabric.ui.custom.quickactions;

import fish.crafting.fimfabric.ui.FancyText;
import fish.crafting.fimfabric.ui.actions.ActionElement;
import fish.crafting.fimfabric.ui.actions.BlockPosActionElement;
import fish.crafting.fimfabric.ui.actions.UIActionList;
import fish.crafting.fimfabric.util.ActionUtils;
import fish.crafting.fimfabric.util.ClickContext;
import fish.crafting.fimfabric.util.VectorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class VectorActions extends UIActionList {

    public VectorActions(int width) {
        super(width);

        addElement(new CopyPlayerVector());
        addSeparator();
        addElement(new CopyTargetBlockVector());
    }

    public static class CopyPlayerVector extends ActionElement {
        public CopyPlayerVector() {
            super(FancyText.of("Copy Player Vector"));
        }

        @Override
        protected void activate(ClickContext context) {
            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if(player == null) return;

            ActionUtils.copyIfClick(context, () -> VectorUtils.toString(player.position()));
        }
    }

    public static class CopyTargetBlockVector extends BlockPosActionElement {

        public CopyTargetBlockVector() {
            super(FancyText.of("Copy Target-Block Vector"));
        }

        @Override
        protected void activate(ClickContext context, BlockPos pos) {
            ActionUtils.copyIfClick(context, () -> VectorUtils.toString(Vec3.atLowerCornerOf(pos)));
        }
    }
}