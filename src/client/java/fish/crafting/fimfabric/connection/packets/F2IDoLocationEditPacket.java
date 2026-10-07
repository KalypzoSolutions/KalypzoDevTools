package fish.crafting.fimfabric.connection.packets;

import fish.crafting.fimfabric.connection.packetsystem.OutPacket;
import fish.crafting.fimfabric.connection.packetsystem.PacketId;
import fish.crafting.fimfabric.editor.values.EditorLocation;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;

public class F2IDoLocationEditPacket extends OutPacket {

    private static final PacketId ID = new PacketId("f2i_edit_location");
    private final EditorLocation location;

    public F2IDoLocationEditPacket(EditorLocation location){
        this.location = location;
    }

    @Override
    protected PacketId getId() {
        return ID;
    }

    @Override
    protected void write(ByteBufOutputStream stream) throws IOException {
        Vec3 pos = location.getPos();

        int decimals = 3; //Oh god
        int mul = (int) Math.pow(10, decimals);

        stream.writeDouble((int) (pos.x * mul) / (double) mul);
        stream.writeDouble((int) (pos.y * mul) / (double) mul);
        stream.writeDouble((int) (pos.z * mul) / (double) mul);

        stream.writeFloat((int) (location.pitch * mul) / (float) mul);
        stream.writeFloat((int) (location.yaw * mul) / (float) mul);
    }
}
