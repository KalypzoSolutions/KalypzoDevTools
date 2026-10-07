package fish.crafting.fimfabric.connection.packets;

import fish.crafting.fimfabric.connection.packetsystem.OutPacket;
import fish.crafting.fimfabric.connection.packetsystem.PacketId;
import fish.crafting.fimfabric.editor.values.EditorBoundingBox;
import fish.crafting.fimfabric.editor.values.EditorLocation;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;

public class F2IDoBoundingBoxEditPacket extends OutPacket {

    private static final PacketId ID = new PacketId("f2i_edit_boundingbox");
    private final EditorBoundingBox box;
    //3 decimals
    private static final int DECIMALS_MUL = (int) Math.pow(10, 5);

    public F2IDoBoundingBoxEditPacket(EditorBoundingBox box){
        this.box = box;
    }

    @Override
    protected PacketId getId() {
        return ID;
    }

    @Override
    protected void write(ByteBufOutputStream stream) throws IOException {
        Vec3 min = box.min;
        Vec3 max = box.max;
        stream.writeDouble(decimals(min.x));
        stream.writeDouble(decimals(min.y));
        stream.writeDouble(decimals(min.z));
        stream.writeDouble(decimals(max.x));
        stream.writeDouble(decimals(max.y));
        stream.writeDouble(decimals(max.z));
    }

    //rip
    private static double decimals(double number) {
        return (int) (number * DECIMALS_MUL) / (double) DECIMALS_MUL;
    }
}
