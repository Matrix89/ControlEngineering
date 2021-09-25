package malte0811.controlengineering.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;

public class PacketUtils {
    public static <T> List<T> readList(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> readElement) {
        int numElements = buffer.readVarInt();
        List<T> ret = new ArrayList<>(numElements);
        for (int i = 0; i < numElements; ++i) {
            ret.add(readElement.apply(buffer));
        }
        return ret;
    }

    public static <T> void writeList(FriendlyByteBuf buffer, List<T> toWrite, BiConsumer<T, FriendlyByteBuf> writeElement) {
        buffer.writeVarInt(toWrite.size());
        for (T element : toWrite) {
            writeElement.accept(element, buffer);
        }
    }
}
