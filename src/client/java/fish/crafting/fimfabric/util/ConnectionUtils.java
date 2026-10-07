package fish.crafting.fimfabric.util;

import fish.crafting.fimfabric.editor.values.EditorBoundingBox;
import fish.crafting.fimfabric.editor.values.EditorLocation;
import fish.crafting.fimfabric.editor.values.EditorVector;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/*
 GUIDE FOR WILDCARDS
 0 - Vector
 1 - Location
 2 - BoundingBox
*/
public class ConnectionUtils {

    public static void writeVector(ByteBufOutputStream stream, EditorVector vector) throws IOException {
        Vec3 vec = vector.vector;
        stream.writeDouble(vec.x);
        stream.writeDouble(vec.y);
        stream.writeDouble(vec.z);
    }

    public static EditorVector readVector(ByteBufInputStream stream) throws IOException {
        return new EditorVector(
                stream.readDouble(),
                stream.readDouble(),
                stream.readDouble()
        );
    }
    public static EditorLocation readLocation(ByteBufInputStream stream) throws IOException {
        return new EditorLocation(
                stream.readDouble(),
                stream.readDouble(),
                stream.readDouble(),
                stream.readFloat(),
                stream.readFloat(),
                stream.readUTF()
        );
    }
    public static EditorBoundingBox readBoundingBox(ByteBufInputStream stream) throws IOException {
        return new EditorBoundingBox(
                stream.readDouble(),
                stream.readDouble(),
                stream.readDouble(),
                stream.readDouble(),
                stream.readDouble(),
                stream.readDouble()
        );
    }

    public static @Nullable Object readWildcard(ByteBufInputStream stream) throws IOException {
        int i = stream.readInt();
        switch (i) {
            case 0 -> {
                return readVector(stream);
            }
            case 1 -> {
                return readLocation(stream);
            }
            case 2 -> {
                return readBoundingBox(stream);
            }
        }

        return null;
    }

}
