package fish.crafting.fimfabric.connection.packets;

import fish.crafting.fimfabric.connection.packetsystem.OutPacket;
import fish.crafting.fimfabric.connection.packetsystem.PacketId;
import fish.crafting.fimfabric.editor.values.EditorVector;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;

public class F2IDoVectorEditPacket extends OutPacket {

    private static final PacketId ID = new PacketId("f2i_edit_vector");
    private final EditorVector vector;

    public F2IDoVectorEditPacket(EditorVector vector){
        this.vector = vector;
    }

    @Override
    protected PacketId getId() {
        return ID;
    }

    @Override
    protected void write(ByteBufOutputStream stream) throws IOException {
        Vec3 pos = vector.getPos();

        int decimals = 3; //Oh god
        int mul = (int) Math.pow(10, decimals);

        stream.writeDouble((int) (pos.x * mul) / (double) mul);
        stream.writeDouble((int) (pos.y * mul) / (double) mul);
        stream.writeDouble((int) (pos.z * mul) / (double) mul);
    }
}
